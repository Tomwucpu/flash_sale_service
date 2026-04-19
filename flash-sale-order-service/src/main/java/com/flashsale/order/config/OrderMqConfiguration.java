package com.flashsale.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderMqConfiguration {

    @Bean
    TopicExchange flashSaleEventExchange() {
        return new TopicExchange("flash.sale.event.exchange", true, false);
    }

    @Bean
    Queue orderCreateQueue() {
        return QueueBuilder.durable("flash.sale.order.create.queue").build();
    }

    @Bean
    Binding orderCreateBinding(Queue orderCreateQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(flashSaleEventExchange).with("order.create");
    }
}
