package com.wanhe.apartmenthub.dto;

import jakarta.validation.constraints.NotNull;

public class RepairVerifyRequest {
    @NotNull
    private Long orderId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}

