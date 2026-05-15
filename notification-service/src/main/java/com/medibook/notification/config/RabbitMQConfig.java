package com.medibook.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification-service (CONSUMER side).
 * Must mirror appointment-service's exchange, queues, and bindings exactly.
 */
@Configuration
public class RabbitMQConfig {

    /* ── Exchange (must match appointment-service) ────────── */
    public static final String EXCHANGE = "medibook.exchange";

    /* ── Queues (must match appointment-service) ──────────── */
    public static final String QUEUE_BOOKED    = "medibook.appointment.booked";
    public static final String QUEUE_CANCELLED = "medibook.appointment.cancelled";
    public static final String QUEUE_COMPLETED = "medibook.appointment.completed";

    /* ── Routing Keys (must match appointment-service) ────── */
    public static final String KEY_BOOKED    = "appointment.booked";
    public static final String KEY_CANCELLED = "appointment.cancelled";
    public static final String KEY_COMPLETED = "appointment.completed";

    /* ── Exchange Bean ────────────────────────────────────── */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    /* ── Queue Beans ──────────────────────────────────────── */
    @Bean public Queue bookedQueue()    { return new Queue(QUEUE_BOOKED,    true); }
    @Bean public Queue cancelledQueue() { return new Queue(QUEUE_CANCELLED, true); }
    @Bean public Queue completedQueue() { return new Queue(QUEUE_COMPLETED, true); }

    /* ── Bindings ─────────────────────────────────────────── */
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

    /* ── JSON Converter ───────────────────────────────────── */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}