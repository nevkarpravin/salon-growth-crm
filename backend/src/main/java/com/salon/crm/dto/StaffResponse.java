package com.salon.crm.dto;

import com.salon.crm.entity.StaffRole;

import java.util.List;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String name,
        StaffRole role,
        String phone,
        String colorHex,
        boolean active,
        List<WorkingHoursDto> workingHours
) {
    public record WorkingHoursDto(String dayOfWeek, String startTime, String endTime) {
    }
}
