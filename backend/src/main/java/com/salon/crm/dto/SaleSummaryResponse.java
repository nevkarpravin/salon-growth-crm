package com.salon.crm.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SaleSummaryResponse(
        BigDecimal revenue,
        long saleCount,
        BigDecimal avgTicket,
        BigDecimal serviceRevenue,
        BigDecimal retailRevenue,
        BigDecimal tipTotal,
        Map<String, BigDecimal> byMethod
) {
}
