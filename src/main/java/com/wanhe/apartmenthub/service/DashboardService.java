package com.wanhe.apartmenthub.service;

import com.wanhe.apartmenthub.domain.RepairStatus;
import com.wanhe.apartmenthub.model.DashboardSummary;
import com.wanhe.apartmenthub.repository.CleaningPlanRepository;
import com.wanhe.apartmenthub.repository.RepairOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

@Service
public class DashboardService {
    private static final int RECENT_REPAIR_LIMIT = 10;

    private final RepairOrderRepository repairOrderRepository;
    private final CleaningPlanRepository cleaningPlanRepository;

    public DashboardService(
            RepairOrderRepository repairOrderRepository,
            CleaningPlanRepository cleaningPlanRepository
    ) {
        this.repairOrderRepository = repairOrderRepository;
        this.cleaningPlanRepository = cleaningPlanRepository;
    }

    public DashboardSummary summary() {
        LocalDate today = LocalDate.now();
        Map<RepairStatus, Long> repairCounts = new EnumMap<>(RepairStatus.class);
        for (RepairStatus status : RepairStatus.values()) {
            repairCounts.put(status, 0L);
        }
        for (RepairOrderRepository.StatusCount count : repairOrderRepository.countByStatus()) {
            repairCounts.put(count.status(), count.total());
        }

        long total = repairCounts.values().stream().mapToLong(Long::longValue).sum();
        return new DashboardSummary(
                today,
                LocalDateTime.now(),
                total,
                repairCounts.get(RepairStatus.PENDING),
                repairCounts.get(RepairStatus.PROCESSING),
                repairCounts.get(RepairStatus.WAITING_CHECK),
                repairCounts.get(RepairStatus.COMPLETED),
                repairOrderRepository.countOpenUrgent(),
                repairOrderRepository.findPage(0, RECENT_REPAIR_LIMIT, null, null, null),
                cleaningPlanRepository.findByPlanDate(today)
        );
    }
}
