package com.salon.crm.controller;

import com.salon.crm.dto.CancelTicketRequest;
import com.salon.crm.dto.JoinQueueRequest;
import com.salon.crm.dto.MarkPaidRequest;
import com.salon.crm.dto.QueueBoardResponse;
import com.salon.crm.dto.QueueTicketResponse;
import com.salon.crm.dto.ReviewRequest;
import com.salon.crm.entity.QueueTicketStatus;
import com.salon.crm.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping("/board")
    public QueueBoardResponse board() {
        return queueService.board();
    }

    @GetMapping
    public List<QueueTicketResponse> listToday(
            @RequestParam(required = false) Set<QueueTicketStatus> status) {
        return queueService.listToday(status);
    }

    @PostMapping
    public ResponseEntity<QueueTicketResponse> join(@Valid @RequestBody JoinQueueRequest req) {
        String source = req.source() != null && !req.source().isBlank() ? req.source() : "DESK";
        JoinQueueRequest withSource = new JoinQueueRequest(req.phone(), req.name(),
                req.serviceIds(), req.staffId(), source);
        return ResponseEntity.status(HttpStatus.CREATED).body(queueService.join(withSource));
    }

    @GetMapping("/{id}")
    public QueueTicketResponse get(@PathVariable UUID id) {
        return queueService.status(id);
    }

    @GetMapping("/by-phone/{phone}")
    public QueueTicketResponse byPhone(@PathVariable String phone) {
        return queueService.statusByPhone(phone);
    }

    @PostMapping("/{id}/call")
    public QueueTicketResponse call(@PathVariable UUID id) {
        return queueService.call(id);
    }

    @PostMapping("/{id}/start")
    public QueueTicketResponse start(@PathVariable UUID id) {
        return queueService.start(id);
    }

    @PostMapping("/{id}/finish")
    public QueueTicketResponse finish(@PathVariable UUID id) {
        return queueService.finish(id);
    }

    @PostMapping("/{id}/skip")
    public QueueTicketResponse skip(@PathVariable UUID id) {
        return queueService.skip(id);
    }

    @PostMapping("/{id}/leave")
    public QueueTicketResponse leave(@PathVariable UUID id) {
        return queueService.leave(id);
    }

    @PostMapping("/{id}/cancel")
    public QueueTicketResponse cancel(@PathVariable UUID id,
                                      @RequestBody(required = false) CancelTicketRequest req) {
        return queueService.cancel(id, req != null ? req.reason() : null);
    }

    @PostMapping("/{id}/payments/mark-paid")
    public QueueTicketResponse markPaid(@PathVariable UUID id,
                                        @Valid @RequestBody MarkPaidRequest req) {
        return queueService.markPaid(id, req.mode(), req.reference());
    }

    @PostMapping("/{id}/review")
    public QueueTicketResponse submitReview(@PathVariable UUID id,
                                            @Valid @RequestBody ReviewRequest req) {
        return queueService.submitReview(id, req.rating(), req.comment());
    }

    @PostMapping("/{id}/review/request")
    public QueueTicketResponse requestReview(@PathVariable UUID id) {
        return queueService.requestReview(id);
    }
}
