package com.scorebroadcaster.service;

import com.scorebroadcaster.dto.CreateMatchRequest;
import com.scorebroadcaster.dto.MatchDTO;
import com.scorebroadcaster.dto.MatchDetailDTO;
import com.scorebroadcaster.dto.ScoreEventDTO;
import com.scorebroadcaster.dto.ScoreUpdateDTO;
import com.scorebroadcaster.entity.EventType;
import com.scorebroadcaster.entity.Match;
import com.scorebroadcaster.entity.MatchStatus;
import com.scorebroadcaster.entity.ScoreEvent;
import com.scorebroadcaster.exception.MatchNotFoundException;
import com.scorebroadcaster.repository.MatchRepository;
import com.scorebroadcaster.repository.ScoreEventRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Core business logic for match and score event management.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final ScoreEventRepository scoreEventRepository;
    private final ModelMapper modelMapper;

    public List<MatchDTO> getAllLiveMatches() {
        return matchRepository.findByStatus(MatchStatus.LIVE).stream()
                .map(match -> toMatchDTO(match, scoreEventRepository.findTop1ByMatchIdOrderByCreatedAtDesc(match.getId())))
                .toList();
    }

    public MatchDetailDTO getMatchWithHistory(Long matchId) {
        Match match = findMatchOrThrow(matchId);
        List<ScoreEvent> events = scoreEventRepository.findByMatchIdOrderByMinuteAsc(matchId);
        Optional<ScoreEvent> latest = scoreEventRepository.findTop1ByMatchIdOrderByCreatedAtDesc(matchId);

        return MatchDetailDTO.builder()
                .id(match.getId())
                .homeTeam(match.getHomeTeam())
                .awayTeam(match.getAwayTeam())
                .matchDate(match.getMatchDate())
                .venue(match.getVenue())
                .status(match.getStatus())
                .homeScore(latest.map(ScoreEvent::getHomeScore).orElse(0))
                .awayScore(latest.map(ScoreEvent::getAwayScore).orElse(0))
                .minute(latest.map(ScoreEvent::getMinute).orElse(0))
                .history(events.stream().map(this::toScoreEventDTO).toList())
                .build();
    }

    @Transactional
    public MatchDTO createMatch(CreateMatchRequest request) {
        Match match = new Match();
        match.setHomeTeam(request.getHomeTeam());
        match.setAwayTeam(request.getAwayTeam());
        match.setMatchDate(request.getMatchDate());
        match.setVenue(request.getVenue());
        match.setStatus(request.getStatus() != null ? request.getStatus() : MatchStatus.SCHEDULED);

        Match saved = matchRepository.save(match);
        return toMatchDTO(saved, Optional.empty());
    }

    @Transactional
    public ScoreEventDTO recordScoreEvent(Long matchId, ScoreUpdateDTO dto) {
        Match match = findMatchOrThrow(matchId);
        Optional<ScoreEvent> previous = scoreEventRepository.findTop1ByMatchIdOrderByCreatedAtDesc(matchId);

        String scoringTeam = resolveScoringTeam(match, dto, previous.orElse(null));
        EventType eventType = resolveEventType(dto, previous.orElse(null));

        ScoreEvent event = new ScoreEvent();
        event.setMatch(match);
        event.setHomeScore(dto.getHomeScore());
        event.setAwayScore(dto.getAwayScore());
        event.setMinute(dto.getMinute());
        event.setEventType(eventType);
        event.setScoringTeam(scoringTeam);

        ScoreEvent saved = scoreEventRepository.save(event);

        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            matchRepository.save(match);
        }

        return toScoreEventDTO(saved);
    }

    @Transactional
    public MatchDTO updateMatchStatus(Long matchId, MatchStatus status) {
        Match match = findMatchOrThrow(matchId);
        match.setStatus(status);
        Match saved = matchRepository.save(match);
        return toMatchDTO(saved, scoreEventRepository.findTop1ByMatchIdOrderByCreatedAtDesc(matchId));
    }

    private Match findMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    private MatchDTO toMatchDTO(Match match, Optional<ScoreEvent> latestEvent) {
        MatchDTO dto = modelMapper.map(match, MatchDTO.class);
        dto.setHomeScore(latestEvent.map(ScoreEvent::getHomeScore).orElse(0));
        dto.setAwayScore(latestEvent.map(ScoreEvent::getAwayScore).orElse(0));
        dto.setMinute(latestEvent.map(ScoreEvent::getMinute).orElse(0));
        return dto;
    }

    private ScoreEventDTO toScoreEventDTO(ScoreEvent event) {
        return ScoreEventDTO.builder()
                .id(event.getId())
                .matchId(event.getMatch().getId())
                .homeScore(event.getHomeScore())
                .awayScore(event.getAwayScore())
                .minute(event.getMinute())
                .eventType(event.getEventType())
                .scoringTeam(event.getScoringTeam())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private String resolveScoringTeam(Match match, ScoreUpdateDTO dto, ScoreEvent previous) {
        if (previous == null) {
            if (dto.getHomeScore() > 0 && dto.getAwayScore() == 0) {
                return match.getHomeTeam();
            }
            if (dto.getAwayScore() > 0 && dto.getHomeScore() == 0) {
                return match.getAwayTeam();
            }
            return "Kick-off";
        }
        if (dto.getHomeScore() > previous.getHomeScore()) {
            return match.getHomeTeam();
        }
        if (dto.getAwayScore() > previous.getAwayScore()) {
            return match.getAwayTeam();
        }
        return "Score update";
    }

    private EventType resolveEventType(ScoreUpdateDTO dto, ScoreEvent previous) {
        if (previous == null) {
            return EventType.GOAL;
        }
        if (dto.getHomeScore() > previous.getHomeScore() || dto.getAwayScore() > previous.getAwayScore()) {
            return EventType.GOAL;
        }
        return EventType.GOAL;
    }
}
