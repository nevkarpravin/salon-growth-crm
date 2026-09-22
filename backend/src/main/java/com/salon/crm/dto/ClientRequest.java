package com.salon.crm.dto;

import com.salon.crm.entity.Gender;
import com.salon.crm.entity.PreferredChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record ClientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 200) String email,
        LocalDate dateOfBirth,
        Gender gender,
        String allergies,
        String notes,
        String preferences,
        Set<String> tags,
        Boolean smsOptIn,
        Boolean whatsappOptIn,
        Boolean emailOptIn,
        Instant marketingConsentAt,
        PreferredChannel preferredChannel
) {
}
