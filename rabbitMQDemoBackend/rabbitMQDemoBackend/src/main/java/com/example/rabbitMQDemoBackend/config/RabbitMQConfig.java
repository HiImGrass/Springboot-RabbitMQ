
package com.example.rabbitMQDemoBackend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Main queue
    public static final String QUEUE_NAME = "demo.queue";
    public static final String EXCHANGE_NAME = "demo.exchange";
    public static final String ROUTING_KEY = "demo.message";

    // Dead Letter Exchange
    public static final String DLX_EXCHANGE_NAME = "demo.dlx";
    public static final String DLQ_QUEUE_NAME = "demo.dlq";
    public static final String DLQ_ROUTING_KEY = "demo.dead";

    @Bean
    public Queue demoQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .deadLetterExchange(DLX_EXCHANGE_NAME)
                .deadLetterRoutingKey(DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange demoExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding demoBinding(
            Queue demoQueue,
            DirectExchange demoExchange
    ) {
        return BindingBuilder
                .bind(demoQueue)
                .to(demoExchange)
                .with(ROUTING_KEY);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE_NAME);
    }

    // Dead Letter Queue
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE_NAME).build();
    }

    // Bind DLQ to DLX
    @Bean
    public Binding deadLetterBinding(
            Queue deadLetterQueue,
            DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }
}
