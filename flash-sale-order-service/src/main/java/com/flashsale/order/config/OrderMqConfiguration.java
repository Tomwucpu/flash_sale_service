package com.flashsale.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单模块 RabbitMQ 消息队列配置
 * 定义了秒杀系统的全局事件交换机以及订单流转相关的各个队列和绑定关系
 */
@Configuration
public class OrderMqConfiguration {

    // ---------------- 全局事件主题交换机 ----------------
    @Bean
    TopicExchange flashSaleEventExchange() {
        return new TopicExchange("flash.sale.event.exchange", true, false);
    }

    // ---------------- 1. 订单创建队列及绑定 ----------------
    @Bean
    Queue orderCreateQueue() {
        return QueueBuilder.durable("flash.sale.order.create.queue").build();
    }

    @Bean
    Binding orderCreateBinding(Queue orderCreateQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(flashSaleEventExchange).with("order.create");
    }

    // ---------------- 2. 支付成功队列及绑定 ----------------
    @Bean
    Queue paymentSuccessQueue() {
        return QueueBuilder.durable("flash.sale.payment.success.queue").build();
    }

    @Bean
    Binding paymentSuccessBinding(Queue paymentSuccessQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(flashSaleEventExchange).with("payment.success");
    }

    // ---------------- 3. 订单超时关闭队列及绑定 ----------------
    @Bean
    Queue orderTimeoutCloseQueue() {
        return QueueBuilder.durable("flash.sale.order.timeout.close.queue").build();
    }

    @Bean
    Binding orderTimeoutCloseBinding(Queue orderTimeoutCloseQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(orderTimeoutCloseQueue).to(flashSaleEventExchange).with("order.timeout.close");
    }

    // ---------------- 4. 导出任务生成队列及绑定 ----------------
    @Bean
    Queue exportGenerateQueue() {
        return QueueBuilder.durable("flash.sale.export.generate.queue").build();
    }

    @Bean
    Binding exportGenerateBinding(Queue exportGenerateQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(exportGenerateQueue).to(flashSaleEventExchange).with("export.generate");
    }

    // ---------------- 5. 导出任务生成死信队列及绑定 ----------------
    @Bean
    Queue exportGenerateDeadQueue() {
        return QueueBuilder.durable("flash.sale.export.generate.dead.queue").build();
    }

    @Bean
    Binding exportGenerateDeadBinding(Queue exportGenerateDeadQueue, TopicExchange flashSaleEventExchange) {
        return BindingBuilder.bind(exportGenerateDeadQueue).to(flashSaleEventExchange).with("export.generate.dead");
    }
}
