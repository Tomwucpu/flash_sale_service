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

    @Bean
    Queue paymentSuccessQueue() {
        return QueueBuilder.durable("flash.sale.payment.success.queue").build();
    }

    @Bean
    Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(flashSaleEventExchange).with("payment.success");
    }

    @Bean
    Queue orderTimeoutCloseQueue() {
        return QueueBuilder.durable("flash.sale.order.timeout.close.queue").build();
    }

    @Bean
    Binding orderTimeoutCloseBinding(Queue orderTimeoutCloseQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(orderTimeoutCloseQueue).to(flashSaleEventExchange).with("order.timeout.close");
    }

    @Bean
    Queue exportGenerateQueue() {
        return QueueBuilder.durable("flash.sale.export.generate.queue").build();
    }

    @Bean
    Binding exportGenerateBinding(Queue exportGenerateQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(exportGenerateQueue).to(flashSaleEventExchange).with("export.generate");
    }

    @Bean
    Queue exportGenerateDeadQueue() {
        return QueueBuilder.durable("flash.sale.export.generate.dead.queue").build();
    }

    @Bean
    Binding exportGenerateDeadBinding(Queue exportGenerateDeadQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(exportGenerateDeadQueue).to(flashSaleEventExchange).with("export.generate.dead");
    }
}
