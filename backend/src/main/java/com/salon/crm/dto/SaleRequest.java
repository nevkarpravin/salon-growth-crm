package com.salon.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SaleRequest(
        UUID clientId,
        UUID staffId,
        UUID appointmentId,
        @Valid List<SaleLineRequest> lines,
        @PositiveOrZero BigDecimal discountAmount,
        @PositiveOrZero BigDecimal tipAmount,
        String notes
) {
}
