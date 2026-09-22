package com.salon.crm.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VisitRequest(
        @NotNull LocalDate visitDate,
        String stylistName,
        List<String> services,
        List<String> products,
        BigDecimal amount,
        String notes
) {
}
