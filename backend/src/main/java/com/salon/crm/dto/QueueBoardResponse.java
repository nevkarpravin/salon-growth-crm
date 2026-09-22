package com.salon.crm.dto;

import java.time.Instant;
import java.util.List;

public record QueueBoardResponse(
        List<QueueTicketResponse> waiting,
        List<QueueTicketResponse> inService,
        List<QueueTicketResponse> completed,
        int activeStaff,
        Instant asOf) {
}
