package com.salon.crm.dto;

import com.salon.crm.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
