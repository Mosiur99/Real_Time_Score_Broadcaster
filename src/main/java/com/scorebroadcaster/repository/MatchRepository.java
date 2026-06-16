package com.scorebroadcaster.repository;

import com.scorebroadcaster.entity.Match;
import com.scorebroadcaster.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Persistence access for {@link Match} entities.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByStatus(MatchStatus status);

    List<Match> findByHomeTeamOrAwayTeam(String homeTeam, String awayTeam);
}
