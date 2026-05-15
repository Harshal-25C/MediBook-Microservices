package com.medibook.appointment.config;

import org.springframework.amqp.core.*;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for appointment-service (PRODUCER side).
 *
 * Exchange  : medibook.exchange  (topic exchange)
 * Queues    :
 *   medibook.appointment.booked     → notification-service listens
 *   medibook.appointment.cancelled  → notification-service listens
 *   medibook.appointment.completed  → notification-service listens
 *
 * Why topic exchange?
 *   Routing key pattern "appointment.*" lets any future consumer
 *   subscribe to all appointment events without changing this config.
 */
@Configuration
public class RabbitMQConfig {

    /* ── Exchange ─────────────────────────────────────────── */
    public static final String EXCHANGE = "medibook.exchange";

    /* ── Queues ───────────────────────────────────────────── */
    public static final String QUEUE_BOOKED    = "medibook.appointment.booked";
    public static final String QUEUE_CANCELLED = "medibook.appointment.cancelled";
    public static final String QUEUE_COMPLETED = "medibook.appointment.completed";

    /* ── Routing Keys ─────────────────────────────────────── */
    public static final String KEY_BOOKED    = "appointment.booked";
    public static final String KEY_CANCELLED = "appointment.cancelled";
    public static final String KEY_COMPLETED = "appointment.completed";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean public Queue bookedQueue()    { return new Queue(QUEUE_BOOKED,    true); }
    @Bean public Queue cancelledQueue() { return new Queue(QUEUE_CANCELLED, true); }
    @Bean public Queue completedQueue() { return new Queue(QUEUE_COMPLETED, true); }

    @Bean
    public Binding bookedBinding(Queue bookedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookedQueue).to(exchange).with(KEY_BOOKED);
    }
    @Bean
    public Binding cancelledBinding(Queue cancelledQueue, TopicExchange exchange) {
        return BindingBuilder.bind(cancelledQueue).to(exchange).with(KEY_CANCELLED);
    }
    @Bean
    public Binding completedBinding(Queue completedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(completedQueue).to(exchange).with(KEY_COMPLETED);
    }

    /** Serialize messages as JSON (not Java serialization) */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}