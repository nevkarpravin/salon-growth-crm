package com.salon.crm.controller;

import com.salon.crm.dto.PaymentRequest;
import com.salon.crm.dto.SaleRequest;
import com.salon.crm.dto.SaleResponse;
import com.salon.crm.dto.SaleSummaryResponse;
import com.salon.crm.entity.SaleStatus;
import com.salon.crm.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class SalesController {

    private final SaleService saleService;

    public SalesController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping
    public Page<SaleResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) UUID clientId,
            @PageableDefault(size = 20) Pageable pageable) {
        return saleService.list(
                from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : null,
                to != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null,
                status, clientId, pageable);
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.createDraft(req));
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable UUID id) {
        return saleService.get(id);
    }

    @PutMapping("/{id}")
    public SaleResponse update(@PathVariable UUID id, @Valid @RequestBody SaleRequest req) {
        return saleService.updateDraft(id, req);
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<SaleResponse> addPayment(@PathVariable UUID id,
                                                   @Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.addPayment(id, req));
    }

    @DeleteMapping("/{id}/payments/{paymentId}")
    public SaleResponse removePayment(@PathVariable UUID id, @PathVariable UUID paymentId) {
        return saleService.removePayment(id, paymentId);
    }

    @PostMapping("/{id}/pay")
    public SaleResponse pay(@PathVariable UUID id) {
        return saleService.pay(id);
    }

    @PostMapping("/{id}/void")
    public SaleResponse voidSale(@PathVariable UUID id) {
        return saleService.voidSale(id);
    }

    @PostMapping("/{id}/refund")
    public SaleResponse refund(@PathVariable UUID id) {
        return saleService.refund(id);
    }

    @PostMapping("/from-appointment/{appointmentId}")
    public ResponseEntity<SaleResponse> fromAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saleService.createFromAppointment(appointmentId));
    }

    @GetMapping("/summary")
    public SaleSummaryResponse summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return saleService.summary(
                from.atStartOfDay().toInstant(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
