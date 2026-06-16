<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>${match.homeTeam} vs ${match.awayTeam}</title>
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

        .match-header {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 1rem;
            padding: 2rem 1.5rem;
        }

        .score-line {
            font-size: clamp(2rem, 8vw, 3.5rem);
            font-weight: 800;
            letter-spacing: 0.04em;
        }

        .team-label {
            font-size: clamp(1rem, 4vw, 1.35rem);
            font-weight: 600;
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

        .live-badge.connected .pulse-dot { animation: pulse 1.4s ease-in-out infinite; }
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

        .timeline {
            position: relative;
            padding-left: 1.5rem;
            border-left: 2px solid var(--card-border);
        }

        .timeline-item {
            position: relative;
            padding-bottom: 1.5rem;
        }

        .timeline-item::before {
            content: '';
            position: absolute;
            left: -1.65rem;
            top: 0.35rem;
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background: var(--accent);
            box-shadow: 0 0 0 3px rgba(0, 230, 118, 0.2);
        }

        .timeline-minute {
            font-weight: 700;
            color: var(--accent);
            min-width: 2.5rem;
        }

        .timeline-card {
            background: var(--card-bg);
            border: 1px solid var(--card-border);
            border-radius: 0.75rem;
            padding: 0.85rem 1rem;
        }

        .timeline-card.new-event {
            border-color: var(--accent);
            animation: highlight 1.2s ease;
        }

        @keyframes highlight {
            from { background: rgba(0, 230, 118, 0.12); }
            to { background: var(--card-bg); }
        }

        @media (max-width: 576px) {
            .match-header { padding: 1.25rem 1rem; }
        }
    </style>
</head>
<body>
<div class="container py-4">
    <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-4">
        <a href="<c:url value='/scores/live'/>" class="btn btn-outline-light btn-sm">&larr; All matches</a>
        <span id="connectionBadge" class="live-badge disconnected">
            <span class="pulse-dot"></span>
            <span id="connectionLabel">Connecting…</span>
        </span>
    </div>

    <section class="match-header text-center mb-4" id="matchHeader">
        <div class="mb-2">
            <span class="badge text-bg-success" id="matchStatus">${match.status}</span>
            <span class="badge text-bg-secondary ms-1" id="matchMinute">${match.minute}'</span>
        </div>
        <div class="row align-items-center g-3">
            <div class="col-4 text-end">
                <div class="team-label" id="homeTeam">${match.homeTeam}</div>
            </div>
            <div class="col-4">
                <div class="score-line" id="scoreLine">
                    <span id="homeScore">${match.homeScore}</span>
                    <span class="text-secondary"> - </span>
                    <span id="awayScore">${match.awayScore}</span>
                </div>
            </div>
            <div class="col-4 text-start">
                <div class="team-label" id="awayTeam">${match.awayTeam}</div>
            </div>
        </div>
    </section>

    <section>
        <h2 class="h5 mb-3">Score timeline</h2>
        <c:choose>
            <c:when test="${empty match.history}">
                <p class="text-secondary">No score events recorded yet.</p>
            </c:when>
            <c:otherwise>
                <div class="timeline" id="scoreTimeline">
                    <c:forEach var="event" items="${match.history}">
                        <div class="timeline-item">
                            <div class="d-flex gap-3">
                                <span class="timeline-minute">${event.minute}'</span>
                                <div class="timeline-card flex-grow-1">
                                    <div class="fw-semibold">${event.scoringTeam}</div>
                                    <div class="text-secondary small">
                                        Score: ${event.homeScore} - ${event.awayScore}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
<script>
    (function () {
        const RECONNECT_DELAY_MS = 5000;
        const MATCH_ID = ${match.id};
        const TOPIC = '/topic/scores/' + MATCH_ID;
        const HOME_TEAM = '<c:out value="${match.homeTeam}"/>';
        const AWAY_TEAM = '<c:out value="${match.awayTeam}"/>';

        let stompClient = null;
        let reconnectTimer = null;
        let lastHomeScore = ${match.homeScore};
        let lastAwayScore = ${match.awayScore};

        function setConnectionState(connected) {
            const badge = document.getElementById('connectionBadge');
            const label = document.getElementById('connectionLabel');
            badge.classList.toggle('connected', connected);
            badge.classList.toggle('disconnected', !connected);
            label.textContent = connected ? 'Live' : 'Reconnecting…';
        }

        function resolveScorer(data) {
            if (data.homeScore > lastHomeScore) return data.homeTeam || HOME_TEAM;
            if (data.awayScore > lastAwayScore) return data.awayTeam || AWAY_TEAM;
            return 'Score update';
        }

        function updateHeader(data) {
            document.getElementById('homeScore').textContent = data.homeScore;
            document.getElementById('awayScore').textContent = data.awayScore;
            document.getElementById('matchMinute').textContent = data.minute + "'";
            if (data.homeTeam) document.getElementById('homeTeam').textContent = data.homeTeam;
            if (data.awayTeam) document.getElementById('awayTeam').textContent = data.awayTeam;
        }

        function appendTimelineEvent(data) {
            let timeline = document.getElementById('scoreTimeline');
            if (!timeline) {
                const section = document.querySelector('section:last-of-type');
                const empty = section.querySelector('p.text-secondary');
                if (empty) empty.remove();
                timeline = document.createElement('div');
                timeline.id = 'scoreTimeline';
                timeline.className = 'timeline';
                section.appendChild(timeline);
            }

            const scorer = resolveScorer(data);
            const item = document.createElement('div');
            item.className = 'timeline-item';
            item.innerHTML =
                '<div class="d-flex gap-3">' +
                    '<span class="timeline-minute">' + data.minute + "'</span>" +
                    '<div class="timeline-card flex-grow-1 new-event">' +
                        '<div class="fw-semibold">' + escapeHtml(scorer) + '</div>' +
                        '<div class="text-secondary small">Score: ' + data.homeScore + ' - ' + data.awayScore + '</div>' +
                    '</div>' +
                '</div>';
            timeline.appendChild(item);

            lastHomeScore = data.homeScore;
            lastAwayScore = data.awayScore;
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        function handleScoreUpdate(data) {
            if (!data || Number(data.matchId) !== MATCH_ID) return;
            updateHeader(data);
            appendTimelineEvent(data);
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
                stompClient.subscribe(TOPIC, function (message) {
                    try {
                        handleScoreUpdate(JSON.parse(message.body));
                    } catch (err) {
                        console.error('Failed to parse score update', err);
                    }
                });
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
