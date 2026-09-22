package com.salon.crm.controller;

import com.salon.crm.dto.QueueTicketResponse;
import com.salon.crm.dto.ReviewRequest;
import com.salon.crm.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/queue")
public class PublicQueueController {

    private final QueueService queueService;

    public PublicQueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/{id}")
    public QueueTicketResponse get(@PathVariable UUID id) {
        return mask(queueService.status(id));
    }

    @PostMapping("/{id}/leave")
    public QueueTicketResponse leave(@PathVariable UUID id) {
        return mask(queueService.leave(id));
    }

    @PostMapping("/{id}/review")
    public QueueTicketResponse review(@PathVariable UUID id,
                                      @Valid @RequestBody ReviewRequest req) {
        return mask(queueService.submitReview(id, req.rating(), req.comment()));
    }

    private QueueTicketResponse mask(QueueTicketResponse r) {
        String phone = r.clientPhone();
        String masked = phone != null && phone.length() > 4
                ? "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4)
                : phone;
        return new QueueTicketResponse(r.id(), r.tokenNumber(), r.queueDate(), r.status(),
                r.clientId(), r.clientName(), masked, r.staffId(), r.staffName(), r.services(),
                r.position(), r.etaMinutes(), r.peopleAhead(), r.skipCount(), r.source(),
                r.joinedAt(), r.calledAt(), r.startedAt(), r.finishedAt(), r.amount(),
                r.paymentStatus(), r.paymentMode(), r.paymentLink(), r.paidAt(), r.rating(),
                r.reviewComment(), r.publicUrl());
    }
}
