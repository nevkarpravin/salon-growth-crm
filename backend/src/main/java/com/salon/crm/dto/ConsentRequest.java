package com.salon.crm.dto;

import com.salon.crm.entity.PreferredChannel;

import java.time.Instant;

public record ConsentRequest(
        boolean smsOptIn,
        boolean whatsappOptIn,
        boolean emailOptIn,
        Instant marketingConsentAt,
        PreferredChannel preferredChannel
) {
}
