package com.wanhe.apartmenthub.controller;

import com.wanhe.apartmenthub.domain.RepairStatus;
import com.wanhe.apartmenthub.domain.RepairType;
import com.wanhe.apartmenthub.dto.RepairAssignRequest;
import com.wanhe.apartmenthub.dto.RepairCompleteRequest;
import com.wanhe.apartmenthub.dto.RepairReportRequest;
import com.wanhe.apartmenthub.dto.RepairVerifyRequest;
import com.wanhe.apartmenthub.model.PageResult;
import com.wanhe.apartmenthub.model.RepairOrder;
import com.wanhe.apartmenthub.service.RepairService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repair")
@Validated
public class RepairController {
    private final RepairService repairService;

    public RepairController(RepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping("/page")
    public PageResult<RepairOrder> page(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) RepairStatus status,
            @RequestParam(required = false) RepairType type
    ) {
        return repairService.page(page, size, status, type);
    }

    @PostMapping("/report")
    public RepairOrder report(@Valid @RequestBody RepairReportRequest request) {
        return repairService.report(request);
    }

    @PutMapping("/assign")
    public RepairOrder assign(@Valid @RequestBody RepairAssignRequest request) {
        return repairService.assign(request);
    }

    @PutMapping("/complete")
    public RepairOrder complete(@Valid @RequestBody RepairCompleteRequest request) {
        return repairService.complete(request);
    }

    @PutMapping("/verify")
    public RepairOrder verify(@Valid @RequestBody RepairVerifyRequest request) {
        return repairService.verify(request);
    }
}
