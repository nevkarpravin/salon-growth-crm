package com.salon.crm.dto;

import com.salon.crm.entity.PaymentMode;
import com.salon.crm.entity.PaymentStatus;
import com.salon.crm.entity.QueueTicketStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record QueueTicketResponse(
        UUID id,
        int tokenNumber,
        LocalDate queueDate,
        QueueTicketStatus status,
        UUID clientId,
        String clientName,
        String clientPhone,
        UUID staffId,
        String staffName,
        List<ServiceRef> services,
        Integer position,
        Integer etaMinutes,
        Integer peopleAhead,
        int skipCount,
        String source,
        Instant joinedAt,
        Instant calledAt,
        Instant startedAt,
        Instant finishedAt,
        BigDecimal amount,
        PaymentStatus paymentStatus,
        PaymentMode paymentMode,
        String paymentLink,
        Instant paidAt,
        Integer rating,
        String reviewComment,
        String publicUrl) {

    public record ServiceRef(UUID id, String name, int durationMinutes, BigDecimal price) {
    }
}
