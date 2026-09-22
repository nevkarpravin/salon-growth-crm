package com.salon.crm.dto;

import com.salon.crm.entity.StaffRole;
import jakarta.validation.constraints.NotBlank;

public record StaffRequest(
        @NotBlank String name,
        StaffRole role,
        String phone,
        String colorHex,
        Boolean active
) {
}
