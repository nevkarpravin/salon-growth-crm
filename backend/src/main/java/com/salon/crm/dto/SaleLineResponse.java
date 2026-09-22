package com.salon.crm.dto;

import com.salon.crm.entity.SaleLineType;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleLineResponse(
        UUID id,
        SaleLineType type,
        UUID refId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal lineTotal
) {
}
