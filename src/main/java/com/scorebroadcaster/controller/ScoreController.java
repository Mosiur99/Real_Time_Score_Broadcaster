package com.scorebroadcaster.controller;

import com.scorebroadcaster.dto.MatchDTO;
import com.scorebroadcaster.dto.MatchDetailDTO;
import com.scorebroadcaster.dto.ScoreUpdateDTO;
import com.scorebroadcaster.service.MatchService;
import com.scorebroadcaster.service.ScorePublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST, WebSocket, and JSP endpoints for score publishing and live views.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ScoreController {

    private final MatchService matchService;
    private final ScorePublisherService scorePublisherService;

    @GetMapping("/")
    public String redirectToLiveScores() {
        return "redirect:/scores/live";
    }

    @GetMapping("/scores/live")
    public String liveScores(Model model) {
        model.addAttribute("matches", matchService.getAllLiveMatches());
        return "live-scores";
    }

    @GetMapping("/scores/match/{matchId}")
    public String matchDetail(@PathVariable Long matchId, Model model) {
        model.addAttribute("match", matchService.getMatchWithHistory(matchId));
        return "match-detail";
    }

    @PostMapping("/api/scores/publish")
    @ResponseBody
    public ResponseEntity<ScoreUpdateDTO> publishScore(@Valid @RequestBody ScoreUpdateDTO dto) {
        ScoreUpdateDTO published = processAndPublish(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(published);
    }

    @GetMapping("/api/scores/live")
    @ResponseBody
    public List<MatchDTO> getLiveMatches() {
        return matchService.getAllLiveMatches();
    }

    @GetMapping("/api/scores/match/{matchId}")
    @ResponseBody
    public MatchDetailDTO getMatchHistory(@PathVariable Long matchId) {
        return matchService.getMatchWithHistory(matchId);
    }

    @MessageMapping("/score.update")
    public void handleWebSocketScoreUpdate(ScoreUpdateDTO dto) {
        log.info("Received score update via WebSocket — matchId={}", dto.getMatchId());
        processAndPublish(dto);
    }

    private ScoreUpdateDTO processAndPublish(ScoreUpdateDTO dto) {
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }
        scorePublisherService.publishScoreUpdate(dto);
        return dto;
    }
}
