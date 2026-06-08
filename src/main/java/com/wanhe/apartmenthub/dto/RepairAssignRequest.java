package com.wanhe.apartmenthub.dto;

import jakarta.validation.constraints.NotNull;

public class RepairAssignRequest {
    @NotNull
    private Long orderId;

    @NotNull
    private Long assigneeId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}

