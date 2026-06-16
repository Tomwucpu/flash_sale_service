package com.flashsale.order.mq;

import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.order.service.OrderProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderEventConsumer {

    private final OrderProcessingService orderProcessingService;

    public PaymentOrderEventConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    @RabbitListener(queues = "${flash-sale.mq.payment-success.queue:flash.sale.payment.success.queue}")
    public void onPaymentSuccess(DomainEvent<?> event) {
        orderProcessingService.handlePaymentSuccess(PaymentSuccessPayload.from(event));
    }
}
