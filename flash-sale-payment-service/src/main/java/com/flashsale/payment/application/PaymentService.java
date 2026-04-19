package com.flashsale.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.mq.event.DomainEvent;
import com.flashsale.payment.domain.OrderRecordEntity;
import com.flashsale.payment.domain.PaymentRecordEntity;
import com.flashsale.payment.mapper.OrderRecordMapper;
import com.flashsale.payment.mapper.PaymentRecordMapper;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final String PAY_STATUS_INIT = "INIT";
    private static final String PAY_STATUS_SUCCESS = "SUCCESS";
    private static final String ORDER_PAY_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String ORDER_PAY_STATUS_PAID = "PAID";
    private static final String ORDER_PAY_STATUS_CLOSED = "CLOSED";
    private static final String ORDER_PAY_STATUS_NO_NEED = "NO_NEED";
    private static final String EVENT_EXCHANGE = "flash.sale.event.exchange";
    private static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";

    private final OrderRecordMapper orderRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentTransactionNoGenerator paymentTransactionNoGenerator;
    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final long paymentTimeoutMillis;
    private final String orderTimeoutDelayQueue;

    public PaymentService(
            OrderRecordMapper orderRecordMapper,
            PaymentRecordMapper paymentRecordMapper,
            PaymentTransactionNoGenerator paymentTransactionNoGenerator,
            RabbitTemplate rabbitTemplate,
            Clock clock,
            ObjectMapper objectMapper,
            @Value("${flash-sale.payment.timeout-ms:900000}") long paymentTimeoutMillis,
            @Value("${flash-sale.mq.order-timeout-delay-queue:flash.sale.order.timeout.delay.queue}") String orderTimeoutDelayQueue
    ) {
        this.orderRecordMapper = orderRecordMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.paymentTransactionNoGenerator = paymentTransactionNoGenerator;
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.paymentTimeoutMillis = paymentTimeoutMillis;
        this.orderTimeoutDelayQueue = orderTimeoutDelayQueue;
    }

    @Transactional
    public PaymentOrderView createPayment(String orderNo, Long currentUserId) {
        OrderRecordEntity order = requireOrder(orderNo);
        if (!order.getUserId().equals(currentUserId)) {
            throw new com.flashsale.common.security.exception.ForbiddenException("无权为该订单发起支付");
        }
        validatePayableOrder(order);

        PaymentRecordEntity existing = paymentRecordMapper.findLatestByOrderNo(orderNo);
        if (existing != null) {
            return toView(existing);
        }

        PaymentRecordEntity entity = new PaymentRecordEntity();
        entity.setOrderNo(orderNo);
        entity.setTransactionNo(paymentTransactionNoGenerator.nextTransactionNo());
        entity.setPayAmount(order.getPriceAmount() == null ? BigDecimal.ZERO : order.getPriceAmount());
        entity.setPayStatus(PAY_STATUS_INIT);
        entity.setCreatedBy(currentUserId);
        entity.setUpdatedBy(currentUserId);
        entity.setIsDeleted(0);
        paymentRecordMapper.insert(entity);

        publishTimeoutEvent(orderNo, entity.getTransactionNo());
        return toView(entity);
    }

    @Transactional
    public PaymentOrderView handleCallback(String orderNo, String transactionNo, Map<String, Object> callbackPayload) {
        OrderRecordEntity order = requireOrder(orderNo);
        PaymentRecordEntity paymentRecord = paymentRecordMapper.findByTransactionNo(transactionNo);
        if (paymentRecord == null) {
            throw new IllegalArgumentException("支付流水不存在");
        }
        if (!paymentRecord.getOrderNo().equals(orderNo)) {
            throw new IllegalArgumentException("支付流水与订单不匹配");
        }
        if (ORDER_PAY_STATUS_CLOSED.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("订单已关闭，不能再支付");
        }
        if (ORDER_PAY_STATUS_NO_NEED.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("该订单无需支付");
        }
        if (PAY_STATUS_SUCCESS.equals(paymentRecord.getPayStatus())) {
            return toView(paymentRecord);
        }

        paymentRecord.setPayStatus(PAY_STATUS_SUCCESS);
        paymentRecord.setPaidAt(LocalDateTime.now(clock));
        paymentRecord.setCallbackPayload(writeJson(callbackPayload));
        paymentRecord.setUpdatedBy(order.getUserId());
        paymentRecordMapper.updateById(paymentRecord);

        publishPaymentSuccessEvent(orderNo, transactionNo);
        return toView(paymentRecordMapper.selectById(paymentRecord.getId()));
    }

    private OrderRecordEntity requireOrder(String orderNo) {
        OrderRecordEntity order = orderRecordMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private void validatePayableOrder(OrderRecordEntity order) {
        if (ORDER_PAY_STATUS_NO_NEED.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("该订单无需支付");
        }
        if (ORDER_PAY_STATUS_PAID.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("该订单已支付");
        }
        if (ORDER_PAY_STATUS_CLOSED.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("该订单已关闭");
        }
        if (!ORDER_PAY_STATUS_WAIT_PAY.equals(order.getPayStatus())) {
            throw new IllegalArgumentException("当前订单状态不允许支付");
        }
    }

    private void publishTimeoutEvent(String orderNo, String transactionNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("transactionNo", transactionNo);
        DomainEvent<Map<String, Object>> event = DomainEvent.create("order.timeout.close", orderNo, payload, clock);
        MessagePostProcessor delayProcessor = message -> {
            message.getMessageProperties().setExpiration(String.valueOf(paymentTimeoutMillis));
            return message;
        };
        rabbitTemplate.convertAndSend("", orderTimeoutDelayQueue, event, delayProcessor);
    }

    private void publishPaymentSuccessEvent(String orderNo, String transactionNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("transactionNo", transactionNo);
        DomainEvent<Map<String, Object>> event = DomainEvent.create("payment.success", orderNo, payload, clock);
        rabbitTemplate.convertAndSend(EVENT_EXCHANGE, PAYMENT_SUCCESS_ROUTING_KEY, event);
    }

    private String writeJson(Map<String, Object> callbackPayload) {
        try {
            return objectMapper.writeValueAsString(callbackPayload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("支付回调序列化失败", exception);
        }
    }

    private PaymentOrderView toView(PaymentRecordEntity entity) {
        return new PaymentOrderView(
                entity.getOrderNo(),
                entity.getTransactionNo(),
                entity.getPayAmount(),
                entity.getPayStatus()
        );
    }

    public record PaymentOrderView(String orderNo, String transactionNo, BigDecimal payAmount, String payStatus) {
    }
}
