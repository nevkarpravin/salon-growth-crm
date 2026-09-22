package com.salon.crm.controller;

import com.salon.crm.dto.PaymentWebhookRequest;
import com.salon.crm.dto.QueueTicketResponse;
import com.salon.crm.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final QueueService queueService;

    public PaymentController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<QueueTicketResponse> webhook(@RequestBody PaymentWebhookRequest req) {
        if (req.ticketId() == null || req.status() == null
                || !req.status().equalsIgnoreCase("PAID")) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                queueService.markPaid(req.ticketId(), req.mode(), req.reference()));
    }
}
