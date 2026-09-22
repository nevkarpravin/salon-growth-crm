package com.salon.crm.dto;

import com.salon.crm.entity.SaleLineType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleLineRequest(
        @NotNull SaleLineType type,
        @NotNull UUID refId,
        @NotNull @Positive Integer quantity,
        @PositiveOrZero BigDecimal discountAmount
) {
}
