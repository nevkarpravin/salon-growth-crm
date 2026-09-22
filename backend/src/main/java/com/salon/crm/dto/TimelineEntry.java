package com.salon.crm.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TimelineEntry(
        String type,
        UUID id,
        LocalDate date,
        String title,
        String subtitle,
        BigDecimal amount,
        List<String> items,
        String notes
) {
}
