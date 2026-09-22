package com.salon.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        String sku,
        String category,
        @NotNull @PositiveOrZero BigDecimal price,
        Integer stockQty,
        Integer lowStockThreshold,
        Boolean active
) {
}
