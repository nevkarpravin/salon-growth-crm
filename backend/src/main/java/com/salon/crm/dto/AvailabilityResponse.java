package com.salon.crm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(List<Slot> slots) {
    public record Slot(LocalDateTime start, LocalDateTime end) {
    }
}
