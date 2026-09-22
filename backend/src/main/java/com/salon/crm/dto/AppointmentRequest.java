package com.salon.crm.dto;

import com.salon.crm.entity.AppointmentSource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull UUID clientId,
        @NotNull UUID staffId,
        @NotNull LocalDateTime startTime,
        @NotEmpty List<UUID> serviceIds,
        AppointmentSource source,
        String notes
) {
}
