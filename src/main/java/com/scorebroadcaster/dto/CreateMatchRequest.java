package com.scorebroadcaster.dto;

import com.scorebroadcaster.entity.MatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new match.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMatchRequest {

    @NotBlank
    private String homeTeam;

    @NotBlank
    private String awayTeam;

    @NotNull
    private LocalDateTime matchDate;

    private String venue;

    private MatchStatus status;
}
