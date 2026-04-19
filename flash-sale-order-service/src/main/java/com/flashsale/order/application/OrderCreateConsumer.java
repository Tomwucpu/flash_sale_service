package com.flashsale.order.application;

import com.flashsale.common.mq.event.DomainEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreateConsumer {

    private final OrderProcessingService orderProcessingService;

    public OrderCreateConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @RabbitListener(queues = "${flash-sale.mq.order-create.queue:flash.sale.order.create.queue}")
    public void onOrderCreate(DomainEvent<?> event) {
        OrderCreatePayload payload = OrderCreatePayload.from(event);
        if (payload.needPayment()) {
            orderProcessingService.handlePaymentOrder(payload);
            return;
        }
        orderProcessingService.handleFreeOrder(payload);
    }
}
