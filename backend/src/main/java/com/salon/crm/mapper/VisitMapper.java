package com.salon.crm.mapper;

import com.salon.crm.dto.VisitRequest;
import com.salon.crm.dto.VisitResponse;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.Visit;

import java.util.ArrayList;

public final class VisitMapper {

    private VisitMapper() {
    }

    public static Visit toEntity(VisitRequest req, Client client) {
        Visit v = new Visit();
        v.setClient(client);
        v.setVisitDate(req.visitDate());
        v.setStylistName(req.stylistName());
        v.setServices(req.services() != null ? new ArrayList<>(req.services()) : new ArrayList<>());
        v.setProducts(req.products() != null ? new ArrayList<>(req.products()) : new ArrayList<>());
        v.setAmount(req.amount());
        v.setNotes(req.notes());
        return v;
    }

    public static VisitResponse toResponse(Visit v) {
        return new VisitResponse(v.getId(), v.getClient().getId(), v.getVisitDate(),
                v.getStylistName(), v.getServices(), v.getProducts(), v.getAmount(),
                v.getNotes(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
