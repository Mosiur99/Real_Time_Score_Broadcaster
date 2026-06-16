package com.scorebroadcaster.dto;

import com.scorebroadcaster.entity.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Match summary returned by the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDTO {

    private Long id;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private String venue;
    private MatchStatus status;
    private int homeScore;
    private int awayScore;
    private int minute;
}
