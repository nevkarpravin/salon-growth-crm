package com.salon.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record JoinQueueRequest(
        @NotBlank String phone,
        String name,
        @NotEmpty List<UUID> serviceIds,
        UUID staffId,
        String source) {
}
