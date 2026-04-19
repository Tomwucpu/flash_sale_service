package com.flashsale.payment.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentMqConfiguration {

    @Bean
    TopicExchange flashSaleEventExchange() {
        return new TopicExchange("flash.sale.event.exchange", true, false);
    }

    @Bean
    Queue orderTimeoutDelayQueue() {
        return QueueBuilder.durable("flash.sale.order.timeout.delay.queue")
                .withArgument("x-dead-letter-exchange", "flash.sale.event.exchange")
                .withArgument("x-dead-letter-routing-key", "order.timeout.close")
                .build();
    }
}
