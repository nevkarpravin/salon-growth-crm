package com.salon.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FormulaCardRequest(
        @NotBlank String serviceName,
        String formula,
        String notes,
        String recordedBy,
        @NotNull LocalDate recordedAt
) {
}
