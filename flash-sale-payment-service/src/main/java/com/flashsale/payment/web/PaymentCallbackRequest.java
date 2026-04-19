package com.flashsale.payment.web;

import jakarta.validation.constraints.NotBlank;

public class PaymentCallbackRequest {

    @NotBlank
    private String orderNo;

    @NotBlank
    private String transactionNo;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }
}
