package com.wanhe.apartmenthub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class RepairCompleteRequest {
    @NotNull
    private Long orderId;

    @NotNull
    @PositiveOrZero
    private BigDecimal repairFee;

    @NotNull
    @PositiveOrZero
    private BigDecimal materialFee;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getRepairFee() {
        return repairFee;
    }

    public void setRepairFee(BigDecimal repairFee) {
        this.repairFee = repairFee;
    }

    public BigDecimal getMaterialFee() {
        return materialFee;
    }

    public void setMaterialFee(BigDecimal materialFee) {
        this.materialFee = materialFee;
    }
}

