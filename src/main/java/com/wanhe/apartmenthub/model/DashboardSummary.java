package com.wanhe.apartmenthub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummary(
        LocalDate date,
        LocalDateTime generatedAt,
        long totalRepairs,
        long pendingRepairs,
        long processingRepairs,
        long waitingCheckRepairs,
        long completedRepairs,
        long urgentOpenRepairs,
        List<RepairOrder> recentRepairs,
        List<CleaningPlan> todayCleaningPlans
) {
}
