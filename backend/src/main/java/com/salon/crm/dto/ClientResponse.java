package com.salon.crm.dto;

import com.salon.crm.entity.ClientStatus;
import com.salon.crm.entity.Gender;
import com.salon.crm.entity.PreferredChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String firstName,
        String lastName,
        String phone,
        String email,
        LocalDate dateOfBirth,
        Gender gender,
        String allergies,
        String notes,
        String preferences,
        Set<String> tags,
        boolean smsOptIn,
        boolean whatsappOptIn,
        boolean emailOptIn,
        Instant marketingConsentAt,
        PreferredChannel preferredChannel,
        ClientStatus status,
        LocalDate lastVisitDate,
        long visitCount,
        BigDecimal totalSpend,
        List<String> segments,
        Instant createdAt,
        Instant updatedAt
) {
}
