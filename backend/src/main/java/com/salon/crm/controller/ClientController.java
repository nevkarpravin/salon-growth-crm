package com.salon.crm.controller;

import com.salon.crm.dto.ClientRequest;
import com.salon.crm.dto.ClientResponse;
import com.salon.crm.dto.ConsentRequest;
import com.salon.crm.dto.FormulaCardRequest;
import com.salon.crm.dto.FormulaCardResponse;
import com.salon.crm.dto.ImportResult;
import com.salon.crm.dto.SegmentCount;
import com.salon.crm.dto.TagCount;
import com.salon.crm.dto.TagsRequest;
import com.salon.crm.dto.TimelineEntry;
import com.salon.crm.dto.VisitRequest;
import com.salon.crm.dto.VisitResponse;
import com.salon.crm.service.ClientService;
import com.salon.crm.service.CsvImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ClientController {

    private final ClientService clientService;
    private final CsvImportService csvImportService;

    public ClientController(ClientService clientService, CsvImportService csvImportService) {
        this.clientService = clientService;
        this.csvImportService = csvImportService;
    }

    @GetMapping("/clients")
    public Page<ClientResponse> list(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) String tag,
                                     @RequestParam(required = false) String segment,
                                     @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return clientService.list(search, tag, segment, pageable);
    }

    @PostMapping("/clients")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(req));
    }

    @GetMapping("/clients/{id}")
    public ClientResponse get(@PathVariable UUID id) {
        return clientService.get(id);
    }

    @PutMapping("/clients/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientRequest req) {
        return clientService.update(id, req);
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/clients/{id}/tags")
    public ClientResponse updateTags(@PathVariable UUID id, @RequestBody TagsRequest req) {
        return clientService.updateTags(id, req);
    }

    @PutMapping("/clients/{id}/consent")
    public ClientResponse updateConsent(@PathVariable UUID id, @RequestBody ConsentRequest req) {
        return clientService.updateConsent(id, req);
    }

    @GetMapping("/clients/{id}/visits")
    public List<VisitResponse> listVisits(@PathVariable UUID id) {
        return clientService.listVisits(id);
    }

    @PostMapping("/clients/{id}/visits")
    public ResponseEntity<VisitResponse> addVisit(@PathVariable UUID id,
                                                  @Valid @RequestBody VisitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addVisit(id, req));
    }

    @DeleteMapping("/clients/{id}/visits/{visitId}")
    public ResponseEntity<Void> deleteVisit(@PathVariable UUID id, @PathVariable UUID visitId) {
        clientService.deleteVisit(id, visitId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients/{id}/formulas")
    public List<FormulaCardResponse> listFormulas(@PathVariable UUID id) {
        return clientService.listFormulas(id);
    }

    @PostMapping("/clients/{id}/formulas")
    public ResponseEntity<FormulaCardResponse> addFormula(@PathVariable UUID id,
                                                          @Valid @RequestBody FormulaCardRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.addFormula(id, req));
    }

    @PutMapping("/clients/{id}/formulas/{formulaId}")
    public FormulaCardResponse updateFormula(@PathVariable UUID id, @PathVariable UUID formulaId,
                                             @Valid @RequestBody FormulaCardRequest req) {
        return clientService.updateFormula(id, formulaId, req);
    }

    @DeleteMapping("/clients/{id}/formulas/{formulaId}")
    public ResponseEntity<Void> deleteFormula(@PathVariable UUID id, @PathVariable UUID formulaId) {
        clientService.deleteFormula(id, formulaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clients/{id}/timeline")
    public List<TimelineEntry> timeline(@PathVariable UUID id) {
        return clientService.timeline(id);
    }

    @PostMapping("/clients/import")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return csvImportService.importCsv(file);
    }

    @GetMapping("/clients/import/template")
    public ResponseEntity<String> importTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"clients-import-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(CsvImportService.TEMPLATE);
    }

    @GetMapping("/segments")
    public List<SegmentCount> segments() {
        return clientService.segmentCounts();
    }

    @GetMapping("/tags")
    public List<TagCount> tags() {
        return clientService.tagCounts();
    }
}
