package com.scorebroadcaster.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Score update message payload exchanged via RabbitMQ and WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScoreUpdateDTO {

    @JsonProperty("matchId")
    private Long matchId;

    @JsonProperty("homeTeam")
    @NotBlank
    private String homeTeam;

    @JsonProperty("awayTeam")
    @NotBlank
    private String awayTeam;

    @JsonProperty("homeScore")
    @Min(0)
    private int homeScore;

    @JsonProperty("awayScore")
    @Min(0)
    private int awayScore;

    @JsonProperty("minute")
    @Min(0)
    private int minute;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
