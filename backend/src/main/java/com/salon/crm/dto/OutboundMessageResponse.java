package com.salon.crm.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboundMessageResponse(
        UUID id,
        String toPhone,
        String body,
        String channel,
        String status,
        String providerMessageId,
        String error,
        UUID ticketId,
        Instant createdAt) {
}
