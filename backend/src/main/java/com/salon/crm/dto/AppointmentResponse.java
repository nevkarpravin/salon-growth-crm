package com.salon.crm.dto;

import com.salon.crm.entity.AppointmentSource;
import com.salon.crm.entity.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientPhone,
        UUID staffId,
        String staffName,
        String colorHex,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<ServiceRef> services,
        AppointmentStatus status,
        AppointmentSource source,
        String notes,
        BigDecimal totalPrice,
        UUID saleId,
        com.salon.crm.entity.SaleStatus saleStatus
) {
    public record ServiceRef(UUID id, String name, int durationMinutes, BigDecimal price) {
    }
}
