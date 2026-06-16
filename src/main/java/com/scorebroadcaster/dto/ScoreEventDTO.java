package com.scorebroadcaster.dto;

import com.scorebroadcaster.entity.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Score event returned by the service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreEventDTO {

    private Long id;
    private Long matchId;
    private int homeScore;
    private int awayScore;
    private int minute;
    private EventType eventType;
    private String scoringTeam;
    private LocalDateTime createdAt;
}
