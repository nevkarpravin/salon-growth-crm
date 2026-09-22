package com.salon.crm.service;

import com.salon.crm.dto.ClientRequest;
import com.salon.crm.dto.ClientResponse;
import com.salon.crm.dto.ConsentRequest;
import com.salon.crm.dto.FormulaCardRequest;
import com.salon.crm.dto.FormulaCardResponse;
import com.salon.crm.dto.SegmentCount;
import com.salon.crm.dto.TagCount;
import com.salon.crm.dto.TagsRequest;
import com.salon.crm.dto.TimelineEntry;
import com.salon.crm.dto.VisitRequest;
import com.salon.crm.dto.VisitResponse;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.ClientStatus;
import com.salon.crm.entity.FormulaCard;
import com.salon.crm.entity.Visit;
import com.salon.crm.exception.DuplicatePhoneException;
import com.salon.crm.exception.NotFoundException;
import com.salon.crm.mapper.ClientMapper;
import com.salon.crm.mapper.FormulaCardMapper;
import com.salon.crm.mapper.VisitMapper;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.FormulaCardRepository;
import com.salon.crm.repository.VisitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final FormulaCardRepository formulaCardRepository;
    private final SegmentService segmentService;

    public ClientService(ClientRepository clientRepository, VisitRepository visitRepository,
                         FormulaCardRepository formulaCardRepository, SegmentService segmentService) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.formulaCardRepository = formulaCardRepository;
        this.segmentService = segmentService;
    }

    @Transactional(readOnly = true)
    public Page<ClientResponse> list(String search, String tag, String segment, Pageable pageable) {
        if (segment == null || segment.isBlank()) {
            Page<Client> page = clientRepository.search(
                    blankToNull(search), blankToNull(tag), ClientStatus.ACTIVE, pageable);
            return new PageImpl<>(page.getContent().stream().map(this::toResponse).toList(),
                    pageable, page.getTotalElements());
        }
        // segment filter applied post-query: full scan, then paginate
        List<ClientResponse> all = clientRepository.search(
                        blankToNull(search), blankToNull(tag), ClientStatus.ACTIVE,
                        Pageable.unpaged())
                .getContent().stream()
                .map(this::toResponse)
                .filter(r -> r.segments().contains(segment))
                .toList();
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    public ClientResponse create(ClientRequest req) {
        if (clientRepository.existsByPhone(req.phone())) {
            throw new DuplicatePhoneException(req.phone());
        }
        Client saved = clientRepository.save(ClientMapper.toEntity(req));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClientResponse get(UUID id) {
        return toResponse(findActive(id));
    }

    public ClientResponse update(UUID id, ClientRequest req) {
        Client client = findActive(id);
        clientRepository.findByPhone(req.phone()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicatePhoneException(req.phone());
            }
        });
        ClientMapper.apply(client, req);
        return toResponse(clientRepository.save(client));
    }

    public void archive(UUID id) {
        Client client = findActive(id);
        client.setStatus(ClientStatus.ARCHIVED);
        clientRepository.save(client);
    }

    public ClientResponse updateTags(UUID id, TagsRequest req) {
        Client client = findActive(id);
        client.setTags(req.tags() != null ? new LinkedHashSet<>(req.tags()) : new LinkedHashSet<>());
        return toResponse(clientRepository.save(client));
    }

    public ClientResponse updateConsent(UUID id, ConsentRequest req) {
        Client client = findActive(id);
        client.setSmsOptIn(req.smsOptIn());
        client.setWhatsappOptIn(req.whatsappOptIn());
        client.setEmailOptIn(req.emailOptIn());
        client.setMarketingConsentAt(req.marketingConsentAt());
        if (req.preferredChannel() != null) {
            client.setPreferredChannel(req.preferredChannel());
        }
        return toResponse(clientRepository.save(client));
    }

    // Visits

    @Transactional(readOnly = true)
    public List<VisitResponse> listVisits(UUID clientId) {
        findActive(clientId);
        return visitRepository.findByClientIdOrderByVisitDateDesc(clientId).stream()
                .map(VisitMapper::toResponse).toList();
    }

    public VisitResponse addVisit(UUID clientId, VisitRequest req) {
        Client client = findActive(clientId);
        return VisitMapper.toResponse(visitRepository.save(VisitMapper.toEntity(req, client)));
    }

    public void deleteVisit(UUID clientId, UUID visitId) {
        findActive(clientId);
        Visit visit = visitRepository.findByIdAndClientId(visitId, clientId)
                .orElseThrow(() -> new NotFoundException("Visit not found: " + visitId));
        visitRepository.delete(visit);
    }

    // Formula cards

    @Transactional(readOnly = true)
    public List<FormulaCardResponse> listFormulas(UUID clientId) {
        findActive(clientId);
        return formulaCardRepository.findByClientIdOrderByRecordedAtDesc(clientId).stream()
                .map(FormulaCardMapper::toResponse).toList();
    }

    public FormulaCardResponse addFormula(UUID clientId, FormulaCardRequest req) {
        Client client = findActive(clientId);
        return FormulaCardMapper.toResponse(
                formulaCardRepository.save(FormulaCardMapper.toEntity(req, client)));
    }

    public FormulaCardResponse updateFormula(UUID clientId, UUID formulaId, FormulaCardRequest req) {
        findActive(clientId);
        FormulaCard card = formulaCardRepository.findByIdAndClientId(formulaId, clientId)
                .orElseThrow(() -> new NotFoundException("Formula card not found: " + formulaId));
        FormulaCardMapper.apply(card, req);
        return FormulaCardMapper.toResponse(formulaCardRepository.save(card));
    }

    public void deleteFormula(UUID clientId, UUID formulaId) {
        findActive(clientId);
        FormulaCard card = formulaCardRepository.findByIdAndClientId(formulaId, clientId)
                .orElseThrow(() -> new NotFoundException("Formula card not found: " + formulaId));
        formulaCardRepository.delete(card);
    }

    // Timeline

    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(UUID clientId) {
        findActive(clientId);
        List<TimelineEntry> entries = new ArrayList<>();
        for (Visit v : visitRepository.findByClientIdOrderByVisitDateDesc(clientId)) {
            entries.add(new TimelineEntry("VISIT", v.getId(), v.getVisitDate(),
                    v.getStylistName() != null ? "Visit — " + v.getStylistName() : "Visit",
                    null, v.getAmount(), v.getServices(), v.getNotes()));
        }
        for (FormulaCard f : formulaCardRepository.findByClientIdOrderByRecordedAtDesc(clientId)) {
            entries.add(new TimelineEntry("FORMULA", f.getId(), f.getRecordedAt(),
                    "Formula — " + f.getServiceName(), f.getRecordedBy(),
                    null, List.of(), f.getNotes()));
        }
        entries.sort(Comparator.comparing(TimelineEntry::date,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    // Segments & tags

    @Transactional(readOnly = true)
    public List<SegmentCount> segmentCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String seg : List.of(SegmentService.NEW, SegmentService.AT_RISK,
                SegmentService.LAPSED, SegmentService.VIP, SegmentService.BIRTHDAY_THIS_MONTH)) {
            counts.put(seg, 0L);
        }
        for (Client c : clientRepository.search(null, null, ClientStatus.ACTIVE,
                Pageable.unpaged()).getContent()) {
            for (String seg : toResponse(c).segments()) {
                counts.merge(seg, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .map(e -> new SegmentCount(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<TagCount> tagCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Client c : clientRepository.search(null, null, ClientStatus.ACTIVE,
                Pageable.unpaged()).getContent()) {
            for (String tag : c.getTags()) {
                counts.merge(tag, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new TagCount(e.getKey(), e.getValue())).toList();
    }

    // Helpers

    public Client findActive(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Client not found: " + id));
        if (client.getStatus() == ClientStatus.ARCHIVED) {
            throw new NotFoundException("Client not found: " + id);
        }
        return client;
    }

    public ClientResponse toResponse(Client c) {
        LocalDate lastVisit = visitRepository.findLastVisitDate(c.getId());
        long visitCount = visitRepository.countByClientId(c.getId());
        BigDecimal totalSpend = visitRepository.sumAmountByClientId(c.getId());
        List<String> segments = segmentService.segmentsFor(c, lastVisit, visitCount, totalSpend);
        return ClientMapper.toResponse(c, lastVisit, visitCount, totalSpend, segments);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
