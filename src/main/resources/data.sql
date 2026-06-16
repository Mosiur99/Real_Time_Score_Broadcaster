-- Sample matches (LIVE)
INSERT INTO matches (id, home_team, away_team, match_date, venue, status) VALUES
(1, 'Arsenal', 'Chelsea', '2026-06-15 15:00:00', 'Emirates Stadium', 'LIVE'),
(2, 'Liverpool', 'Manchester City', '2026-06-15 17:30:00', 'Anfield', 'LIVE'),
(3, 'Barcelona', 'Real Madrid', '2026-06-15 20:00:00', 'Camp Nou', 'LIVE');

-- Sample score events
INSERT INTO score_events (match_id, home_score, away_score, minute, event_type, scoring_team, created_at) VALUES
(1, 1, 0, 23, 'GOAL', 'Arsenal', '2026-06-15 15:23:00'),
(1, 1, 1, 41, 'GOAL', 'Chelsea', '2026-06-15 15:41:00'),
(1, 2, 1, 67, 'GOAL', 'Arsenal', '2026-06-15 16:07:00'),
(2, 1, 0, 12, 'GOAL', 'Liverpool', '2026-06-15 17:42:00'),
(2, 1, 1, 55, 'PENALTY', 'Manchester City', '2026-06-15 18:25:00');
