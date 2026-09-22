package com.salon.crm.dto;

import jakarta.validation.constraints.NotBlank;

public record SimulateRequest(
        @NotBlank String from,
        @NotBlank String text,
        String profileName) {
}
