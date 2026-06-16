package com.scorebroadcaster.listener;

import com.scorebroadcaster.dto.ScoreUpdateDTO;
import com.scorebroadcaster.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes score updates from the per-instance RabbitMQ queue, pushes them to
 * STOMP subscribers, then persists the score event to MySQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreMessageListener {

    private static final String SCORE_TOPIC_PATTERN = "/topic/scores/";

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchService matchService;

    @RabbitListener(queues = "#{scoreUpdatesInstanceQueue.name}")
    public void onScoreUpdate(ScoreUpdateDTO scoreUpdate) {
        if (scoreUpdate == null) {
            log.warn("Received null score update message — ignoring");
            return;
        }
        if (scoreUpdate.getMatchId() == null) {
            log.warn("Received score update without matchId — ignoring payload={}", scoreUpdate);
            return;
        }

        String destination = SCORE_TOPIC_PATTERN + scoreUpdate.getMatchId();
        boolean webSocketDelivered = false;
        try {
            messagingTemplate.convertAndSend(destination, scoreUpdate);
            webSocketDelivered = true;
            log.info(
                    "Forwarded score update to WebSocket — destination={}, matchId={}, {} {}-{} {}",
                    destination,
                    scoreUpdate.getMatchId(),
                    scoreUpdate.getHomeTeam(),
                    scoreUpdate.getHomeScore(),
                    scoreUpdate.getAwayScore(),
                    scoreUpdate.getAwayTeam()
            );
        } catch (Exception ex) {
            log.error(
                    "Failed to forward score update to WebSocket — destination={}, matchId={}: {}",
                    destination,
                    scoreUpdate.getMatchId(),
                    ex.getMessage(),
                    ex
            );
        }

        try {
            matchService.recordScoreEvent(scoreUpdate.getMatchId(), scoreUpdate);
            log.info("Persisted score event for matchId={} (webSocketDelivered={})",
                    scoreUpdate.getMatchId(), webSocketDelivered);
        } catch (Exception ex) {
            log.error(
                    "Failed to persist score event for matchId={}: {}",
                    scoreUpdate.getMatchId(),
                    ex.getMessage(),
                    ex
            );
        }
    }
}
