package com.scorebroadcaster.config;

import com.rabbitmq.client.ShutdownSignalException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.stereotype.Component;

/**
 * Logs RabbitMQ connection lifecycle events for operational visibility.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitConnectionMonitor {

    private final ConnectionFactory connectionFactory;

    @PostConstruct
    public void registerConnectionListener() {
        if (!(connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory)) {
            log.warn("ConnectionFactory is not a CachingConnectionFactory — connection monitoring disabled");
            return;
        }

        cachingConnectionFactory.addConnectionListener(new ConnectionListener() {
            @Override
            public void onCreate(Connection connection) {
                log.info("RabbitMQ connection established — {}", connectionAddress(connection));
            }

            @Override
            public void onClose(Connection connection) {
                log.warn("RabbitMQ connection closed — {}", connectionAddress(connection));
            }

            @Override
            public void onShutDown(ShutdownSignalException signal) {
                if (signal.isHardError()) {
                    log.error("RabbitMQ connection hard shutdown — reason={}", signal.getMessage(), signal);
                } else {
                    log.warn("RabbitMQ connection soft shutdown — reason={}", signal.getMessage());
                }
            }

            @Override
            public void onFailed(Exception exception) {
                log.error("RabbitMQ connection failed — {}", exception.getMessage(), exception);
            }
        });
    }

    private static String connectionAddress(Connection connection) {
        if (connection.getDelegate() != null && connection.getDelegate().getAddress() != null) {
            return connection.getDelegate().getAddress().toString();
        }
        return String.valueOf(connection);
    }
}
