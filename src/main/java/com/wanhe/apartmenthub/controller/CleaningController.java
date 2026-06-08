package com.wanhe.apartmenthub.controller;

import com.wanhe.apartmenthub.dto.CreateCleaningPlanRequest;
import com.wanhe.apartmenthub.model.CleaningPlan;
import com.wanhe.apartmenthub.service.CleaningService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cleaning")
public class CleaningController {
    private final CleaningService cleaningService;

    public CleaningController(CleaningService cleaningService) {
        this.cleaningService = cleaningService;
    }

    @GetMapping("/plans")
    public List<CleaningPlan> list() {
        return cleaningService.list();
    }

    @PostMapping("/plans")
    public CleaningPlan create(@Valid @RequestBody CreateCleaningPlanRequest request) {
        return cleaningService.create(request);
    }

    @PutMapping("/plans/{id}/start")
    public CleaningPlan start(@PathVariable Long id) {
        return cleaningService.start(id);
    }

    @PutMapping("/plans/{id}/complete")
    public CleaningPlan complete(@PathVariable Long id) {
        return cleaningService.complete(id);
    }

    @PutMapping("/plans/{id}/skip")
    public CleaningPlan skip(@PathVariable Long id) {
        return cleaningService.skip(id);
    }
}

