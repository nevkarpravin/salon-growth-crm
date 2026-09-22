package com.salon.crm.mapper;

import com.salon.crm.dto.ClientRequest;
import com.salon.crm.dto.ClientResponse;
import com.salon.crm.entity.Client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static Client toEntity(ClientRequest req) {
        Client c = new Client();
        apply(c, req);
        return c;
    }

    public static void apply(Client c, ClientRequest req) {
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setDateOfBirth(req.dateOfBirth());
        c.setGender(req.gender());
        c.setAllergies(req.allergies());
        c.setNotes(req.notes());
        c.setPreferences(req.preferences());
        if (req.tags() != null) {
            c.setTags(new LinkedHashSet<>(req.tags()));
        }
        c.setSmsOptIn(Boolean.TRUE.equals(req.smsOptIn()));
        c.setWhatsappOptIn(Boolean.TRUE.equals(req.whatsappOptIn()));
        c.setEmailOptIn(Boolean.TRUE.equals(req.emailOptIn()));
        c.setMarketingConsentAt(req.marketingConsentAt());
        if (req.preferredChannel() != null) {
            c.setPreferredChannel(req.preferredChannel());
        }
    }

    public static ClientResponse toResponse(Client c, LocalDate lastVisit, long visitCount,
                                            BigDecimal totalSpend, List<String> segments) {
        return new ClientResponse(
                c.getId(), c.getFirstName(), c.getLastName(), c.getPhone(), c.getEmail(),
                c.getDateOfBirth(), c.getGender(), c.getAllergies(), c.getNotes(), c.getPreferences(),
                c.getTags(), c.isSmsOptIn(), c.isWhatsappOptIn(), c.isEmailOptIn(),
                c.getMarketingConsentAt(), c.getPreferredChannel(), c.getStatus(),
                lastVisit, visitCount, totalSpend, segments, c.getCreatedAt(), c.getUpdatedAt());
    }
}
