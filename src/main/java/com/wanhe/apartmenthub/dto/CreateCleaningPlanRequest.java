package com.wanhe.apartmenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateCleaningPlanRequest {
    @NotBlank
    @Size(max = 100)
    private String area;

    @NotBlank
    @Size(max = 64)
    private String cleanerName;

    @NotNull
    private LocalDate planDate;

    private LocalTime planTime;

    @Size(max = 500)
    private String remark;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCleanerName() {
        return cleanerName;
    }

    public void setCleanerName(String cleanerName) {
        this.cleanerName = cleanerName;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public LocalTime getPlanTime() {
        return planTime;
    }

    public void setPlanTime(LocalTime planTime) {
        this.planTime = planTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
