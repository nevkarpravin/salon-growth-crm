package com.salon.crm.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceItemResponse(
        UUID id,
        String name,
        String category,
        int durationMinutes,
        int processingMinutes,
        BigDecimal price,
        boolean active
) {
}
