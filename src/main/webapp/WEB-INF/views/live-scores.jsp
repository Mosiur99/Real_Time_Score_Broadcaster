<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Live Scores</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"/>
    <style>
        :root {
            --card-bg: #1a1d29;
            --card-border: #2d3148;
            --accent: #00e676;
            --text-muted: #9aa0b8;
        }

        body {
            background: linear-gradient(160deg, #0f1117 0%, #1a1d29 50%, #12141c 100%);
            min-height: 100vh;
            color: #f0f2f8;
        }

        .page-header {
            border-bottom: 1px solid var(--card-border);
            backdrop-filter: blur(8px);
        }

        .live-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.4rem;
            font-size: 0.75rem;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            padding: 0.35rem 0.75rem;
            border-radius: 999px;
            background: rgba(0, 230, 118, 0.15);
            color: var(--accent);
            border: 1px solid rgba(0, 230, 118, 0.35);
        }

        .live-badge.connected .pulse-dot {
            animation: pulse 1.4s ease-in-out infinite;
        }

        .live-badge.disconnected {
            background: rgba(255, 82, 82, 0.12);
            color: #ff5252;
            border-color: rgba(255, 82, 82, 0.35);
        }

        .pulse-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: currentColor;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.4; transform: scale(0.85); }
        }

        .score-grid {
            display: flex;
            flex-wrap: wrap;
            gap: 1.25rem;
            justify-content: center;
        }

        .score-card {
            flex: 1 1 300px;
            max-width: 380px;
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 1rem;
            padding: 1.5rem;
            transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
        }

        .score-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
            border-color: #3d4260;
        }

        .score-card.updated {
            border-color: var(--accent);
            box-shadow: 0 0 0 1px rgba(0, 230, 118, 0.25);
        }

        .teams-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 0.75rem;
        }

        .team-name {
            flex: 1;
            font-weight: 600;
            font-size: 1rem;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .team-name.away {
            text-align: right;
        }

        .score-display {
            font-size: 2rem;
            font-weight: 800;
            letter-spacing: 0.05em;
            color: #fff;
            min-width: 2.5rem;
            text-align: center;
        }

        .score-separator {
            font-size: 1.5rem;
            color: var(--text-muted);
            font-weight: 300;
        }

        .minute-badge {
            display: inline-block;
            font-size: 0.8rem;
            font-weight: 600;
            padding: 0.25rem 0.65rem;
            border-radius: 0.5rem;
            background: #2d3148;
            color: #c5cae0;
        }

        .empty-state {
            text-align: center;
            padding: 4rem 1rem;
            color: var(--text-muted);
        }

        @media (max-width: 576px) {
            .score-card {
                flex: 1 1 100%;
                max-width: 100%;
            }

            .score-display {
                font-size: 1.75rem;
            }
        }
    </style>
</head>
<body>
<header class="page-header py-3 mb-4">
    <div class="container d-flex flex-wrap align-items-center justify-content-between gap-2">
        <h1 class="h4 mb-0 fw-bold">Live Scores</h1>
        <span id="connectionBadge" class="live-badge disconnected">
            <span class="pulse-dot"></span>
            <span id="connectionLabel">Connecting…</span>
        </span>
    </div>
</header>

<main class="container pb-5">
    <c:choose>
        <c:when test="${empty matches}">
            <div class="empty-state">
                <p class="fs-5 mb-1">No live matches right now</p>
                <p class="small">Score updates will appear here automatically.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="score-grid" id="scoreGrid">
                <c:forEach var="match" items="${matches}">
                    <article class="score-card" id="match-${match.id}" data-match-id="${match.id}">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <span class="badge text-bg-success">${match.status}</span>
                            <span class="minute-badge">${match.minute}'</span>
                        </div>
                        <div class="teams-row">
                            <span class="team-name home-team">${match.homeTeam}</span>
                            <span class="score-display home-score">${match.homeScore}</span>
                            <span class="score-separator">:</span>
                            <span class="score-display away-score">${match.awayScore}</span>
                            <span class="team-name away away-team">${match.awayTeam}</span>
                        </div>
                        <div class="mt-3 text-end">
                            <a href="<c:url value='/scores/match/${match.id}'/>"
                               class="btn btn-sm btn-outline-light">Details</a>
                        </div>
                    </article>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</main>

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
<script>
    (function () {
        const RECONNECT_DELAY_MS = 5000;
        const TOPIC_PREFIX = '/topic/scores/';

        const matchIds = [
            <c:forEach var="match" items="${matches}" varStatus="status">
            ${match.id}<c:if test="${!status.last}">,</c:if>
            </c:forEach>
        ];

        let stompClient = null;
        let reconnectTimer = null;

        function setConnectionState(connected) {
            const badge = document.getElementById('connectionBadge');
            const label = document.getElementById('connectionLabel');
            if (!badge || !label) return;

            badge.classList.toggle('connected', connected);
            badge.classList.toggle('disconnected', !connected);
            label.textContent = connected ? 'Live' : 'Reconnecting…';
        }

        function updateScoreCard(data) {
            const card = document.getElementById('match-' + data.matchId);
            if (!card) return;

            const homeScore = card.querySelector('.home-score');
            const awayScore = card.querySelector('.away-score');
            const minuteBadge = card.querySelector('.minute-badge');
            const homeTeam = card.querySelector('.home-team');
            const awayTeam = card.querySelector('.away-team');

            if (homeScore) homeScore.textContent = data.homeScore;
            if (awayScore) awayScore.textContent = data.awayScore;
            if (minuteBadge) minuteBadge.textContent = data.minute + "'";
            if (homeTeam && data.homeTeam) homeTeam.textContent = data.homeTeam;
            if (awayTeam && data.awayTeam) awayTeam.textContent = data.awayTeam;

            card.classList.add('updated');
            setTimeout(function () { card.classList.remove('updated'); }, 1200);
        }

        function subscribeToTopics(client) {
            matchIds.forEach(function (matchId) {
                client.subscribe(TOPIC_PREFIX + matchId, function (message) {
                    try {
                        updateScoreCard(JSON.parse(message.body));
                    } catch (err) {
                        console.error('Failed to parse score update', err);
                    }
                });
            });
        }

        function connect() {
            const socket = new SockJS('<c:url value="/ws"/>');
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, function () {
                setConnectionState(true);
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }
                subscribeToTopics(stompClient);
            }, function () {
                setConnectionState(false);
                scheduleReconnect();
            });

            socket.onclose = function () {
                setConnectionState(false);
                scheduleReconnect();
            };
        }

        function scheduleReconnect() {
            if (reconnectTimer) return;
            reconnectTimer = setTimeout(function () {
                reconnectTimer = null;
                if (stompClient && stompClient.connected) return;
                connect();
            }, RECONNECT_DELAY_MS);
        }

        connect();
    })();
</script>
</body>
</html>
