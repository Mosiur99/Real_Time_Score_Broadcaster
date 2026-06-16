package com.scorebroadcaster.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;

import java.util.UUID;

/**
 * RabbitMQ configuration for score update broadcasting.
 *
 * <p>Uses a fanout exchange so every WebSocket server instance receives all
 * score updates via its own exclusive, auto-delete queue.</p>
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String SCORES_FANOUT_EXCHANGE = "scores.fanout";

    @Bean
    public FanoutExchange scoresFanoutExchange() {
        return new FanoutExchange(SCORES_FANOUT_EXCHANGE, true, false);
    }

    /**
     * Per-instance queue: exclusive to this JVM connection and removed when the
     * consumer disconnects, so each WebSocket server gets its own copy of every message.
     *
     * <p>Uses {@link QueueBuilder} instead of {@code AnonymousQueue} to avoid declaring
     * {@code x-queue-master-locator}, which RabbitMQ 4.3+ rejects by default.</p>
     */
    @Bean
    public Queue scoreUpdatesInstanceQueue() {
        String queueName = "scores.ws." + UUID.randomUUID();
        return QueueBuilder.nonDurable(queueName)
                .exclusive()
                .autoDelete()
                .build();
    }

    @Bean
    public Binding scoreUpdatesBinding(Queue scoreUpdatesInstanceQueue, FanoutExchange scoresFanoutExchange) {
        return BindingBuilder.bind(scoreUpdatesInstanceQueue).to(scoresFanoutExchange);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setReturnsCallback(returned -> log.error(
                "RabbitMQ message returned — exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("RabbitMQ publish not acknowledged — correlation={}, cause={}",
                        correlationData, cause);
            }
        });
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setErrorHandler(rabbitListenerErrorHandler());
        return factory;
    }

    @Bean
    public ErrorHandler rabbitListenerErrorHandler() {
        FatalExceptionStrategy conversionExceptionStrategy = t -> {
            Throwable cause = unwrapListenerException(t);
            return cause instanceof MessageConversionException;
        };
        ConditionalRejectingErrorHandler handler =
                new ConditionalRejectingErrorHandler(conversionExceptionStrategy);
        handler.setDiscardFatalsWithXDeath(true);
        return t -> {
            Throwable cause = unwrapListenerException(t);
            if (cause instanceof MessageConversionException) {
                log.error("Discarding score update message — JSON conversion failed: {}",
                        cause.getMessage(), cause);
            } else {
                log.error("RabbitMQ listener failed while processing score update", t);
            }
            handler.handleError(t);
        };
    }

    private static Throwable unwrapListenerException(Throwable throwable) {
        if (throwable instanceof ListenerExecutionFailedException failed
                && failed.getCause() != null) {
            return failed.getCause();
        }
        return throwable.getCause() != null ? throwable.getCause() : throwable;
    }
}
