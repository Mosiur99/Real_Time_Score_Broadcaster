package com.scorebroadcaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Real-Time Score Broadcaster application.
 *
 * <p>Integrates Spring Web, JPA, RabbitMQ (fanout), and STOMP WebSocket
 * to publish and deliver live sports score updates to browser clients.</p>
 */
@SpringBootApplication
public class ScoreBroadcasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoreBroadcasterApplication.class, args);
    }
}
