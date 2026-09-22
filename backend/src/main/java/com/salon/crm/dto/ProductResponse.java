package com.salon.crm.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String category,
        BigDecimal price,
        int stockQty,
        int lowStockThreshold,
        boolean active,
        boolean lowStock
) {
}
