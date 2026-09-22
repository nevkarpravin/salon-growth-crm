package com.salon.crm.dto;

import com.salon.crm.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;

public record MarkPaidRequest(
        @NotNull PaymentMode mode,
        String reference) {
}
