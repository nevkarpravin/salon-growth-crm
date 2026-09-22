package com.salon.crm.mapper;

import com.salon.crm.dto.FormulaCardRequest;
import com.salon.crm.dto.FormulaCardResponse;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.FormulaCard;

public final class FormulaCardMapper {

    private FormulaCardMapper() {
    }

    public static FormulaCard toEntity(FormulaCardRequest req, Client client) {
        FormulaCard f = new FormulaCard();
        f.setClient(client);
        apply(f, req);
        return f;
    }

    public static void apply(FormulaCard f, FormulaCardRequest req) {
        f.setServiceName(req.serviceName());
        f.setFormula(req.formula());
        f.setNotes(req.notes());
        f.setRecordedBy(req.recordedBy());
        f.setRecordedAt(req.recordedAt());
    }

    public static FormulaCardResponse toResponse(FormulaCard f) {
        return new FormulaCardResponse(f.getId(), f.getClient().getId(), f.getServiceName(),
                f.getFormula(), f.getNotes(), f.getRecordedBy(), f.getRecordedAt(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
