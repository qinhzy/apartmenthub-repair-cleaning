package com.wanhe.apartmenthub.service;

import com.wanhe.apartmenthub.domain.CleaningStatus;
import com.wanhe.apartmenthub.dto.CreateCleaningPlanRequest;
import com.wanhe.apartmenthub.model.CleaningPlan;
import com.wanhe.apartmenthub.repository.CleaningPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CleaningService {
    private final CleaningPlanRepository cleaningPlanRepository;

    public CleaningService(CleaningPlanRepository cleaningPlanRepository) {
        this.cleaningPlanRepository = cleaningPlanRepository;
    }

    public List<CleaningPlan> list() {
        return cleaningPlanRepository.findAll();
    }

    public CleaningPlan create(CreateCleaningPlanRequest request) {
        long id = cleaningPlanRepository.insert(request);
        return getRequired(id);
    }

    public CleaningPlan start(Long id) {
        CleaningPlan plan = getRequired(id);
        if (plan.getStatus() != CleaningStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only PENDING plans can be started");
        }
        cleaningPlanRepository.updateStatus(id, CleaningStatus.IN_PROGRESS, LocalDateTime.now());
        return getRequired(id);
    }

    public CleaningPlan complete(Long id) {
        CleaningPlan plan = getRequired(id);
        if (plan.getStatus() != CleaningStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only IN_PROGRESS plans can be completed");
        }
        cleaningPlanRepository.updateStatus(id, CleaningStatus.COMPLETED, LocalDateTime.now());
        return getRequired(id);
    }

    public CleaningPlan skip(Long id) {
        CleaningPlan plan = getRequired(id);
        if (plan.getStatus() == CleaningStatus.COMPLETED || plan.getStatus() == CleaningStatus.SKIPPED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "finished plans cannot be skipped");
        }
        cleaningPlanRepository.updateStatus(id, CleaningStatus.SKIPPED, LocalDateTime.now());
        return getRequired(id);
    }

    private CleaningPlan getRequired(Long id) {
        return cleaningPlanRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cleaning plan not found"));
    }
}

