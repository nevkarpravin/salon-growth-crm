package com.salon.crm.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FormulaCardResponse(
        UUID id,
        UUID clientId,
        String serviceName,
        String formula,
        String notes,
        String recordedBy,
        LocalDate recordedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
