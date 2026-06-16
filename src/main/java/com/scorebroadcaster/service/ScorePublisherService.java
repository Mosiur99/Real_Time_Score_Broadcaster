package com.scorebroadcaster.service;

import com.scorebroadcaster.config.RabbitMQConfig;
import com.scorebroadcaster.dto.ScoreUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes score updates to the RabbitMQ fanout exchange for cluster-wide broadcast.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScorePublisherService {

    private static final String FANOUT_ROUTING_KEY = "";

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a score update to all connected WebSocket server instances.
     *
     * @param dto score update payload
     */
    public void publishScoreUpdate(ScoreUpdateDTO dto) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SCORES_FANOUT_EXCHANGE, FANOUT_ROUTING_KEY, dto);
            log.info(
                    "Published score update — matchId={}, {} {}-{} {} (minute {}), timestamp={}",
                    dto.getMatchId(),
                    dto.getHomeTeam(),
                    dto.getHomeScore(),
                    dto.getAwayScore(),
                    dto.getAwayTeam(),
                    dto.getMinute(),
                    dto.getTimestamp()
            );
        } catch (AmqpConnectException ex) {
            log.error(
                    "RabbitMQ connection failure while publishing score update for matchId={}: {}",
                    dto.getMatchId(),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        } catch (AmqpException ex) {
            log.error(
                    "Failed to publish score update for matchId={}: {}",
                    dto.getMatchId(),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }
}
