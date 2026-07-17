package com.wanhe.apartmenthub.service;

import com.wanhe.apartmenthub.domain.RepairStatus;
import com.wanhe.apartmenthub.domain.RepairType;
import com.wanhe.apartmenthub.dto.RepairAssignRequest;
import com.wanhe.apartmenthub.dto.RepairCompleteRequest;
import com.wanhe.apartmenthub.dto.RepairReportRequest;
import com.wanhe.apartmenthub.dto.RepairVerifyRequest;
import com.wanhe.apartmenthub.model.PageResult;
import com.wanhe.apartmenthub.model.RepairOrder;
import com.wanhe.apartmenthub.repository.RepairOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RepairService {
    private static final BigDecimal MAX_TOTAL_FEE = new BigDecimal("99999999.99");

    private final RepairOrderRepository repairOrderRepository;

    public RepairService(RepairOrderRepository repairOrderRepository) {
        this.repairOrderRepository = repairOrderRepository;
    }

    public PageResult<RepairOrder> page(int page, int size, RepairStatus status, RepairType type) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        long offset = (long) (safePage - 1) * safeSize;
        return new PageResult<>(
                repairOrderRepository.findPage(offset, safeSize, status, type),
                safePage,
                safeSize,
                repairOrderRepository.count(status, type)
        );
    }

    public RepairOrder report(RepairReportRequest request) {
        if (!repairOrderRepository.existsUser(request.getReporterId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reporterId does not exist");
        }
        long id = repairOrderRepository.insert(request);
        return getRequired(id);
    }

    @Transactional
    public RepairOrder assign(RepairAssignRequest request) {
        RepairOrder order = getRequired(request.getOrderId());
        if (order.getStatus() != RepairStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only PENDING orders can be assigned");
        }
        if (!repairOrderRepository.existsUser(request.getAssigneeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assigneeId does not exist");
        }

        int updated = repairOrderRepository.assign(
                order.getId(),
                request.getAssigneeId(),
                LocalDateTime.now(),
                RepairStatus.PENDING
        );
        requireSuccessfulTransition(updated);
        return getRequired(order.getId());
    }

    @Transactional
    public RepairOrder complete(RepairCompleteRequest request) {
        RepairOrder order = getRequired(request.getOrderId());
        if (order.getStatus() != RepairStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only PROCESSING orders can be completed");
        }

        BigDecimal totalFee = request.getRepairFee().add(request.getMaterialFee());
        if (totalFee.compareTo(MAX_TOTAL_FEE) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalFee exceeds database limit");
        }
        int updated = repairOrderRepository.complete(
                order.getId(),
                request.getRepairFee(),
                request.getMaterialFee(),
                totalFee,
                LocalDateTime.now(),
                RepairStatus.PROCESSING
        );
        requireSuccessfulTransition(updated);
        return getRequired(order.getId());
    }

    @Transactional
    public RepairOrder verify(RepairVerifyRequest request) {
        RepairOrder order = getRequired(request.getOrderId());
        if (order.getStatus() != RepairStatus.WAITING_CHECK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only WAITING_CHECK orders can be verified");
        }

        int updated = repairOrderRepository.verify(
                order.getId(),
                LocalDateTime.now(),
                RepairStatus.WAITING_CHECK
        );
        requireSuccessfulTransition(updated);
        return getRequired(order.getId());
    }

    private void requireSuccessfulTransition(int updatedRows) {
        if (updatedRows != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "repair order state changed; retry the operation"
            );
        }
    }

    private RepairOrder getRequired(Long id) {
        return repairOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "repair order not found"));
    }
}
