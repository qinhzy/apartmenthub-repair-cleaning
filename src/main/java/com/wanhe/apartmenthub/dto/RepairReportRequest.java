package com.wanhe.apartmenthub.dto;

import com.wanhe.apartmenthub.domain.Priority;
import com.wanhe.apartmenthub.domain.RepairType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RepairReportRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private RepairType repairType;

    @NotNull
    private Priority priority;

    @NotNull
    private Long reporterId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RepairType getRepairType() {
        return repairType;
    }

    public void setRepairType(RepairType repairType) {
        this.repairType = repairType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }
}

