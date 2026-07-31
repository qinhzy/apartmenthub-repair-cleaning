package com.wanhe.apartmenthub.service;

import com.wanhe.apartmenthub.domain.CleaningStatus;
import com.wanhe.apartmenthub.model.CleaningPlan;
import com.wanhe.apartmenthub.repository.CleaningPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleaningServiceConcurrencyTests {
    @Mock
    private CleaningPlanRepository cleaningPlanRepository;

    @InjectMocks
    private CleaningService cleaningService;

    @Test
    void startReturnsConflictWhenStateChangesAfterRead() {
        CleaningPlan plan = new CleaningPlan();
        plan.setId(20L);
        plan.setStatus(CleaningStatus.PENDING);

        when(cleaningPlanRepository.findById(20L)).thenReturn(Optional.of(plan));
        when(cleaningPlanRepository.updateStatus(
                eq(20L),
                eq(CleaningStatus.PENDING),
                eq(CleaningStatus.IN_PROGRESS),
                any(LocalDateTime.class)
        )).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cleaningService.start(20L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
