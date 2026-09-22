package com.salon.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ServiceItemRequest(
        @NotBlank String name,
        String category,
        @NotNull @Positive Integer durationMinutes,
        @PositiveOrZero Integer processingMinutes,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean active
) {
}
