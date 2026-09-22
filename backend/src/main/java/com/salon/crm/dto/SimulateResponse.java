package com.salon.crm.dto;

import java.util.List;

public record SimulateResponse(
        String reply,
        List<OutboundMessageResponse> messages) {
}
