package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutCloseConsumer {

    private final OrderProcessingService orderProcessingService;

    public OrderTimeoutCloseConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @RabbitListener(queues = "${flash-sale.mq.order-timeout-close.queue:flash.sale.order.timeout.close.queue}")
    public void onOrderTimeoutClose(DomainEvent<?> event) {
        orderProcessingService.handleOrderTimeoutClose(OrderTimeoutClosePayload.from(event));
    }
}
