package com.salon.crm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID clientId,
        LocalDate visitDate,
        String stylistName,
        List<String> services,
        List<String> products,
        BigDecimal amount,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
