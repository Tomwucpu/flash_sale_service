package com.flashsale.order.job;

import com.flashsale.order.application.OrderProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "flash-sale.order.timeout-close-scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OrderTimeoutCloseScheduler {

    private final OrderProcessingService orderProcessingService;
    private final long paymentTimeoutMillis;
    private final int batchSize;

    public OrderTimeoutCloseScheduler(
            OrderProcessingService orderProcessingService,
            @Value("${flash-sale.payment.timeout-ms:900000}") long paymentTimeoutMillis,
            @Value("${flash-sale.order.timeout-close-batch-size:100}") int batchSize
    ) {
        this.orderProcessingService = orderProcessingService;
        this.paymentTimeoutMillis = paymentTimeoutMillis;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${flash-sale.order.timeout-close-scan-delay:5000}",
            initialDelayString = "${flash-sale.order.timeout-close-initial-delay:5000}"
    )
    public void closeOverduePaymentOrders() {
        orderProcessingService.closeOverduePaymentOrders(Duration.ofMillis(paymentTimeoutMillis), batchSize);
    }
}
