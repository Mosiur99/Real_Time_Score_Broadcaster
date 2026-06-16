package com.scorebroadcaster.dto;

import com.scorebroadcaster.entity.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Match detail with chronological score event history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailDTO {

    private Long id;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private String venue;
    private MatchStatus status;
    private int homeScore;
    private int awayScore;
    private int minute;
    private List<ScoreEventDTO> history;
}
