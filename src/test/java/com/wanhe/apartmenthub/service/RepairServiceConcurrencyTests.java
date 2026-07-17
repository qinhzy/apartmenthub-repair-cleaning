package com.wanhe.apartmenthub.service;

import com.wanhe.apartmenthub.domain.RepairStatus;
import com.wanhe.apartmenthub.dto.RepairAssignRequest;
import com.wanhe.apartmenthub.model.RepairOrder;
import com.wanhe.apartmenthub.repository.RepairOrderRepository;
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
class RepairServiceConcurrencyTests {
    @Mock
    private RepairOrderRepository repairOrderRepository;

    @InjectMocks
    private RepairService repairService;

    @Test
    void assignReturnsConflictWhenStateChangesAfterRead() {
        RepairOrder order = new RepairOrder();
        order.setId(10L);
        order.setStatus(RepairStatus.PENDING);

        RepairAssignRequest request = new RepairAssignRequest();
        request.setOrderId(10L);
        request.setAssigneeId(2L);

        when(repairOrderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(repairOrderRepository.existsUser(2L)).thenReturn(true);
        when(repairOrderRepository.assign(
                eq(10L),
                eq(2L),
                any(LocalDateTime.class),
                eq(RepairStatus.PENDING)
        )).thenReturn(0);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repairService.assign(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
