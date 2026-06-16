package com.scorebroadcaster.repository;

import com.scorebroadcaster.entity.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link ScoreEvent} entities.
 */
@Repository
public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {

    List<ScoreEvent> findByMatchIdOrderByMinuteAsc(Long matchId);

    Optional<ScoreEvent> findTop1ByMatchIdOrderByCreatedAtDesc(Long matchId);
}
