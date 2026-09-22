package com.salon.crm.dto;

import com.salon.crm.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull PaymentMethod method,
        @NotNull @Positive BigDecimal amount,
        String reference
) {
}
