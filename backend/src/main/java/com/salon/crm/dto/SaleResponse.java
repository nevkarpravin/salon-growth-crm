package com.salon.crm.dto;

import com.salon.crm.entity.PaymentMethod;
import com.salon.crm.entity.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        String invoiceNumber,
        UUID clientId,
        String clientName,
        String clientPhone,
        UUID appointmentId,
        UUID staffId,
        String staffName,
        List<SaleLineResponse> lines,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal total,
        List<PaymentDto> payments,
        SaleStatus status,
        String notes,
        Instant paidAt,
        Instant createdAt,
        List<String> warnings
) {
    public record PaymentDto(UUID id, PaymentMethod method, BigDecimal amount,
                             String reference, Instant paidAt) {
    }
}
