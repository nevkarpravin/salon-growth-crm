package com.salon.crm.dto;

import com.salon.crm.entity.PaymentMode;

import java.util.UUID;

public record PaymentWebhookRequest(
        UUID ticketId,
        String status,
        String reference,
        PaymentMode mode) {
}
