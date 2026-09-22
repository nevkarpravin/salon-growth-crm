package com.salon.crm.service;

import com.salon.crm.config.VirtualRoomProperties;
import com.salon.crm.dto.JoinQueueRequest;
import com.salon.crm.dto.QueueBoardResponse;
import com.salon.crm.dto.QueueTicketResponse;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.ConversationState;
import com.salon.crm.entity.ConversationStep;
import com.salon.crm.entity.PaymentMode;
import com.salon.crm.entity.PaymentStatus;
import com.salon.crm.entity.PreferredChannel;
import com.salon.crm.entity.QueueTicket;
import com.salon.crm.entity.QueueTicketStatus;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.entity.Visit;
import com.salon.crm.exception.BadRequestException;
import com.salon.crm.exception.NotFoundException;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ConversationStateRepository;
import com.salon.crm.repository.QueueTicketRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.VisitRepository;
import com.salon.crm.whatsapp.MessageTemplates;
import com.salon.crm.whatsapp.MessagingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class QueueService {

    private static final Set<QueueTicketStatus> ACTIVE =
            EnumSet.of(QueueTicketStatus.WAITING, QueueTicketStatus.CALLED,
                    QueueTicketStatus.IN_SERVICE);
    private static final Set<QueueTicketStatus> QUEUED =
            EnumSet.of(QueueTicketStatus.WAITING, QueueTicketStatus.CALLED);

    private final QueueTicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StaffRepository staffRepository;
    private final VisitRepository visitRepository;
    private final ConversationStateRepository conversationStateRepository;
    private final MessagingService messagingService;
    private final PaymentService paymentService;
    private final VirtualRoomProperties properties;

    public QueueService(QueueTicketRepository ticketRepository,
                        ClientRepository clientRepository,
                        ServiceItemRepository serviceItemRepository,
                        StaffRepository staffRepository,
                        VisitRepository visitRepository,
                        ConversationStateRepository conversationStateRepository,
                        MessagingService messagingService,
                        PaymentService paymentService,
                        VirtualRoomProperties properties) {
        this.ticketRepository = ticketRepository;
        this.clientRepository = clientRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.staffRepository = staffRepository;
        this.visitRepository = visitRepository;
        this.conversationStateRepository = conversationStateRepository;
        this.messagingService = messagingService;
        this.paymentService = paymentService;
        this.properties = properties;
    }

    // ---- join / status ----

    public QueueTicketResponse join(JoinQueueRequest req) {
        return join(req, true);
    }

    public QueueTicketResponse join(JoinQueueRequest req, boolean sendConfirmation) {
        LocalDate today = LocalDate.now();
        String phone = req.phone().trim();
        Client client = clientRepository.findByPhone(phone).orElseGet(() -> {
            Client c = new Client();
            c.setFirstName(req.name() != null && !req.name().isBlank()
                    ? req.name().trim() : "Guest");
            c.setPhone(phone);
            c.setWhatsappOptIn(true);
            c.setPreferredChannel(PreferredChannel.WHATSAPP);
            c.setMarketingConsentAt(Instant.now());
            return clientRepository.save(c);
        });

        QueueTicket existing = ticketRepository
                .findFirstByClient_PhoneAndStatusInOrderByJoinedAtDesc(phone, ACTIVE)
                .filter(t -> today.equals(t.getQueueDate()))
                .orElse(null);
        if (existing != null) {
            throw new BadRequestException("You are already in the queue as token #"
                    + existing.getTokenNumber());
        }

        List<ServiceItem> services = resolveActiveServices(req.serviceIds());
        Staff staff = null;
        if (req.staffId() != null) {
            staff = staffRepository.findById(req.staffId())
                    .orElseThrow(() -> new NotFoundException("Staff not found: " + req.staffId()));
            if (!staff.isActive()) {
                throw new BadRequestException("Staff member is not active");
            }
        }

        int token = ticketRepository.maxTokenNumberForDate(today) + 1;
        QueueTicket ticket = new QueueTicket();
        ticket.setClient(client);
        ticket.setStaff(staff);
        ticket.setServices(services);
        ticket.setTokenNumber(token);
        ticket.setQueueDate(today);
        ticket.setQueueOrder(token);
        ticket.setStatus(QueueTicketStatus.WAITING);
        ticket.setJoinedAt(Instant.now());
        ticket.setSource(req.source() != null && !req.source().isBlank()
                ? req.source() : "DESK");
        ticket.setAmount(services.stream()
                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ticket = ticketRepository.save(ticket);

        if (sendConfirmation) {
            int[] pos = positionAndEta(ticket);
            messagingService.send(client.getPhone(),
                    MessageTemplates.joinConfirmation(properties.getSalonName(),
                            client.getFirstName(), ticket, pos[0] - 1, pos[1],
                            properties.getPublicBaseUrl()),
                    ticket);
        }

        refreshNotifications();
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse status(UUID id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public QueueTicketResponse statusByPhone(String phone) {
        QueueTicket ticket = ticketRepository
                .findFirstByClient_PhoneAndStatusInOrderByJoinedAtDesc(phone, ACTIVE)
                .filter(t -> LocalDate.now().equals(t.getQueueDate()))
                .orElseThrow(() -> new NotFoundException("No active ticket for phone: " + phone));
        return toResponse(ticket);
    }

    // ---- state changes ----

    public QueueTicketResponse call(UUID id) {
        QueueTicket ticket = find(id);
        require(ticket, QueueTicketStatus.WAITING, "call");
        ticket.setStatus(QueueTicketStatus.CALLED);
        ticket.setCalledAt(Instant.now());
        ticketRepository.save(ticket);
        messagingService.send(ticket.getClient().getPhone(),
                MessageTemplates.yourTurn(ticket), ticket);
        if ("WHATSAPP".equals(ticket.getSource())) {
            conversationStateRepository.findByPhone(ticket.getClient().getPhone())
                    .ifPresent(cs -> {
                        cs.setStep(ConversationStep.CONFIRM_PRESENCE);
                        cs.setActiveTicketId(ticket.getId());
                        conversationStateRepository.save(cs);
                    });
        }
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse start(UUID id) {
        QueueTicket ticket = find(id);
        if (ticket.getStatus() != QueueTicketStatus.WAITING
                && ticket.getStatus() != QueueTicketStatus.CALLED) {
            throw new BadRequestException("Cannot start ticket in status " + ticket.getStatus());
        }
        ticket.setStatus(QueueTicketStatus.IN_SERVICE);
        ticket.setStartedAt(Instant.now());
        ticketRepository.save(ticket);
        messagingService.send(ticket.getClient().getPhone(),
                MessageTemplates.serviceStarted(), ticket);
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse finish(UUID id) {
        QueueTicket ticket = find(id);
        require(ticket, QueueTicketStatus.IN_SERVICE, "finish");
        ticket.setStatus(QueueTicketStatus.COMPLETED);
        ticket.setFinishedAt(Instant.now());
        ticket.setPaymentStatus(PaymentStatus.PENDING);
        ticket.setPaymentLink(paymentService.upiIntentLink(ticket.getAmount(),
                properties.getSalonName() + " token #" + ticket.getTokenNumber()));
        ticketRepository.save(ticket);

        Visit visit = new Visit();
        visit.setClient(ticket.getClient());
        visit.setVisitDate(ticket.getQueueDate());
        visit.setStylistName(ticket.getStaff() != null ? ticket.getStaff().getName() : "Any");
        visit.setServices(ticket.getServices().stream().map(ServiceItem::getName).toList());
        visit.setAmount(ticket.getAmount());
        visitRepository.save(visit);

        messagingService.send(ticket.getClient().getPhone(),
                MessageTemplates.bill(ticket, ticket.getPaymentLink()), ticket);
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse leave(UUID id) {
        QueueTicket ticket = find(id);
        if (ticket.getStatus() != QueueTicketStatus.WAITING
                && ticket.getStatus() != QueueTicketStatus.CALLED) {
            throw new BadRequestException("Cannot leave ticket in status " + ticket.getStatus());
        }
        ticket.setStatus(QueueTicketStatus.CANCELLED);
        ticket.setCancelReason("CLIENT");
        ticket.setClosedAt(Instant.now());
        ticketRepository.save(ticket);
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse cancel(UUID id, String reason) {
        QueueTicket ticket = find(id);
        if (ticket.getStatus() != QueueTicketStatus.WAITING
                && ticket.getStatus() != QueueTicketStatus.CALLED) {
            throw new BadRequestException("Cannot cancel ticket in status " + ticket.getStatus());
        }
        ticket.setStatus(QueueTicketStatus.CANCELLED);
        ticket.setCancelReason(reason != null && !reason.isBlank() ? reason : "SALON");
        ticket.setClosedAt(Instant.now());
        ticketRepository.save(ticket);
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse skip(UUID id) {
        QueueTicket ticket = find(id);
        if (ticket.getStatus() != QueueTicketStatus.WAITING
                && ticket.getStatus() != QueueTicketStatus.CALLED) {
            throw new BadRequestException("Cannot skip ticket in status " + ticket.getStatus());
        }
        ticket.setSkipCount(ticket.getSkipCount() + 1);
        if (ticket.getSkipCount() >= properties.getMaxSkips()) {
            ticket.setStatus(QueueTicketStatus.CANCELLED);
            ticket.setCancelReason("NO_SHOW");
            ticket.setClosedAt(Instant.now());
            ticketRepository.save(ticket);
            messagingService.send(ticket.getClient().getPhone(),
                    MessageTemplates.noShowCancelled(), ticket);
        } else {
            moveBack(ticket);
            int position = positionAndEta(ticket)[0];
            messagingService.send(ticket.getClient().getPhone(),
                    MessageTemplates.movedBack(ticket, position), ticket);
        }
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse delay(UUID id) {
        QueueTicket ticket = find(id);
        require(ticket, QueueTicketStatus.CALLED, "delay");
        moveBack(ticket);
        int position = positionAndEta(ticket)[0];
        messagingService.send(ticket.getClient().getPhone(),
                MessageTemplates.movedBack(ticket, position), ticket);
        refreshNotifications();
        return toResponse(ticket);
    }

    public QueueTicketResponse markPaid(UUID id, PaymentMode mode, String reference) {
        QueueTicket ticket = find(id);
        if (ticket.getPaymentStatus() == PaymentStatus.PAID) {
            return toResponse(ticket); // idempotent
        }
        if (ticket.getStatus() != QueueTicketStatus.COMPLETED
                || ticket.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Cannot mark paid ticket in status "
                    + ticket.getStatus() + " / " + ticket.getPaymentStatus());
        }
        ticket.setPaymentStatus(PaymentStatus.PAID);
        ticket.setPaymentMode(mode != null ? mode : PaymentMode.UPI_LINK);
        ticket.setPaymentReference(reference);
        ticket.setPaidAt(Instant.now());
        ticketRepository.save(ticket);
        messagingService.send(ticket.getClient().getPhone(),
                MessageTemplates.receipt(ticket, ticket.getPaymentMode().name()), ticket);
        requestReviewIfDue(ticket);
        return toResponse(ticket);
    }

    public QueueTicketResponse requestReview(UUID id) {
        QueueTicket ticket = find(id);
        if (ticket.getStatus() != QueueTicketStatus.COMPLETED) {
            throw new BadRequestException("Cannot request review for ticket in status "
                    + ticket.getStatus());
        }
        if (ticket.getReviewRequestedAt() == null) {
            ticket.setReviewRequestedAt(Instant.now());
            ticketRepository.save(ticket);
            messagingService.send(ticket.getClient().getPhone(),
                    MessageTemplates.reviewPrompt(), ticket);
            conversationStateRepository.findByPhone(ticket.getClient().getPhone())
                    .ifPresentOrElse(cs -> {
                        cs.setStep(ConversationStep.REVIEW_RATING);
                        cs.setActiveTicketId(ticket.getId());
                        conversationStateRepository.save(cs);
                    }, () -> {
                        ConversationState cs = new ConversationState();
                        cs.setPhone(ticket.getClient().getPhone());
                        cs.setStep(ConversationStep.REVIEW_RATING);
                        cs.setActiveTicketId(ticket.getId());
                        conversationStateRepository.save(cs);
                    });
        }
        return toResponse(ticket);
    }

    public QueueTicketResponse submitReview(UUID id, int rating, String comment) {
        QueueTicket ticket = find(id);
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be 1-5");
        }
        if (ticket.getRating() == null) {
            ticket.setRating(rating);
        }
        if (comment != null && !comment.isBlank()) {
            ticket.setReviewComment(comment);
        }
        ticket.setReviewedAt(Instant.now());
        ticketRepository.save(ticket);
        String googleUrl = properties.getGoogleReviewUrl();
        String reply = ticket.getRating() != null && ticket.getRating() >= 4
                && googleUrl != null && !googleUrl.isBlank()
                ? MessageTemplates.publicReviewThanks(googleUrl)
                : MessageTemplates.privateReviewThanks();
        messagingService.send(ticket.getClient().getPhone(), reply, ticket);
        return toResponse(ticket);
    }

    public void requestReviewIfDue(QueueTicket ticket) {
        if (properties.getReviewDelayMinutes() == 0
                && ticket.getStatus() == QueueTicketStatus.COMPLETED
                && ticket.getReviewRequestedAt() == null
                && ticket.getRating() == null) {
            requestReview(ticket.getId());
        }
    }

    // ---- listing ----

    @Transactional(readOnly = true)
    public List<QueueTicketResponse> listToday(Set<QueueTicketStatus> statuses) {
        LocalDate today = LocalDate.now();
        List<QueueTicket> tickets = statuses == null || statuses.isEmpty()
                ? ticketRepository.findByQueueDateOrderByQueueOrderAsc(today)
                : ticketRepository.findByQueueDateAndStatusInOrderByQueueOrderAsc(today, statuses);
        return tickets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public QueueBoardResponse board() {
        LocalDate today = LocalDate.now();
        List<QueueTicket> all = ticketRepository.findByQueueDateOrderByQueueOrderAsc(today);
        int activeStaff = activeStaffCount();
        return new QueueBoardResponse(
                all.stream().filter(t -> QUEUED.contains(t.getStatus()))
                        .map(this::toResponse).toList(),
                all.stream().filter(t -> t.getStatus() == QueueTicketStatus.IN_SERVICE)
                        .map(this::toResponse).toList(),
                all.stream().filter(t -> t.getStatus() == QueueTicketStatus.COMPLETED)
                        .map(this::toResponse).toList(),
                activeStaff,
                Instant.now());
    }

    // ---- position / eta ----

    /**
     * @return int[]{position (1-based), etaMinutes}
     */
    public int[] positionAndEta(QueueTicket ticket) {
        List<QueueTicket> today = ticketRepository
                .findByQueueDateOrderByQueueOrderAsc(ticket.getQueueDate());
        boolean samePool = false;
        int peopleAhead = 0;
        double workAhead = 0;
        Instant now = Instant.now();
        for (QueueTicket t : today) {
            if (t.getId().equals(ticket.getId())) {
                continue;
            }
            boolean pool = ticket.getStaff() == null || t.getStaff() == null
                    || t.getStaff().getId().equals(ticket.getStaff().getId());
            if (!pool) {
                continue;
            }
            if (QUEUED.contains(t.getStatus()) && t.getQueueOrder() < ticket.getQueueOrder()) {
                peopleAhead++;
                workAhead += totalDuration(t);
            } else if (t.getStatus() == QueueTicketStatus.IN_SERVICE) {
                samePool = true;
                int total = totalDuration(t);
                long elapsed = t.getStartedAt() != null
                        ? Duration.between(t.getStartedAt(), now).toMinutes() : 0;
                workAhead += Math.max(0, total - elapsed);
            }
        }
        int staffCount = ticket.getStaff() != null ? 1 : Math.max(1, activeStaffCount());
        int eta = (int) Math.round(workAhead / staffCount);
        return new int[]{peopleAhead + 1, eta};
    }

    public void refreshNotifications() {
        LocalDate today = LocalDate.now();
        List<QueueTicket> waiting = ticketRepository
                .findByQueueDateAndStatusInOrderByQueueOrderAsc(today,
                        Set.of(QueueTicketStatus.WAITING));
        for (QueueTicket t : waiting) {
            if (t.getNextNotifiedAt() != null) {
                continue;
            }
            int position = positionAndEta(t)[0];
            if (position <= properties.getNotifyAtPosition()) {
                t.setNextNotifiedAt(Instant.now());
                ticketRepository.save(t);
                messagingService.send(t.getClient().getPhone(),
                        MessageTemplates.youreNext(t), t);
            }
        }
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void expireStale() {
        LocalDate today = LocalDate.now();
        for (QueueTicket t : ticketRepository.findByStatusIn(QUEUED)) {
            expireIfBefore(t, today);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void sendDueReviewPrompts() {
        if (properties.getReviewDelayMinutes() <= 0) {
            return;
        }
        Instant before = Instant.now()
                .minus(Duration.ofMinutes(properties.getReviewDelayMinutes()));
        for (QueueTicket t : ticketRepository.findCompletedAwaitingReview(before, null)) {
            requestReview(t.getId());
        }
    }

    // ---- helpers ----

    public QueueTicket find(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Queue ticket not found: " + id));
    }

    public QueueTicketResponse toResponse(QueueTicket t) {
        Integer position = null;
        Integer eta = null;
        if (QUEUED.contains(t.getStatus())) {
            int[] pos = positionAndEta(t);
            position = pos[0];
            eta = pos[1];
        }
        String clientName = (t.getClient().getFirstName()
                + (t.getClient().getLastName() != null ? " " + t.getClient().getLastName() : ""))
                .trim();
        return new QueueTicketResponse(
                t.getId(),
                t.getTokenNumber(),
                t.getQueueDate(),
                t.getStatus(),
                t.getClient().getId(),
                clientName,
                t.getClient().getPhone(),
                t.getStaff() != null ? t.getStaff().getId() : null,
                t.getStaff() != null ? t.getStaff().getName() : null,
                t.getServices().stream()
                        .map(s -> new QueueTicketResponse.ServiceRef(
                                s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice()))
                        .toList(),
                position,
                eta,
                position != null ? position - 1 : null,
                t.getSkipCount(),
                t.getSource(),
                t.getJoinedAt(),
                t.getCalledAt(),
                t.getStartedAt(),
                t.getFinishedAt(),
                t.getAmount(),
                t.getPaymentStatus(),
                t.getPaymentMode(),
                t.getPaymentLink(),
                t.getPaidAt(),
                t.getRating(),
                t.getReviewComment(),
                properties.getPublicBaseUrl() + "/q/" + t.getId());
    }

    private void moveBack(QueueTicket ticket) {
        LocalDate today = ticket.getQueueDate();
        List<QueueTicket> all = ticketRepository
                .findByQueueDateOrderByQueueOrderAsc(today);
        List<QueueTicket> after = all.stream()
                .filter(t -> t.getStatus() == QueueTicketStatus.WAITING
                        && t.getQueueOrder() > ticket.getQueueOrder())
                .sorted(Comparator.comparingDouble(QueueTicket::getQueueOrder))
                .toList();
        int n = properties.getRequeuePositions();
        double newOrder;
        if (after.size() >= n) {
            double anchor = after.get(n - 1).getQueueOrder();
            if (after.size() > n) {
                newOrder = (anchor + after.get(n).getQueueOrder()) / 2.0;
            } else {
                newOrder = anchor + 0.5;
            }
        } else {
            newOrder = all.stream()
                    .mapToDouble(QueueTicket::getQueueOrder).max().orElse(0) + 1;
        }
        ticket.setQueueOrder(newOrder);
        ticket.setStatus(QueueTicketStatus.WAITING);
        ticket.setNextNotifiedAt(null);
        ticketRepository.save(ticket);
    }

    private void expireIfBefore(QueueTicket t, LocalDate today) {
        if (t.getQueueDate().isBefore(today)
                && QUEUED.contains(t.getStatus())) {
            t.setStatus(QueueTicketStatus.EXPIRED);
            t.setClosedAt(Instant.now());
            ticketRepository.save(t);
        }
    }

    private void require(QueueTicket ticket, QueueTicketStatus expected, String action) {
        if (ticket.getStatus() != expected) {
            throw new BadRequestException("Cannot " + action + " ticket in status "
                    + ticket.getStatus());
        }
    }

    private List<ServiceItem> resolveActiveServices(List<UUID> serviceIds) {
        List<ServiceItem> services = serviceItemRepository.findByIdIn(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new NotFoundException("One or more services not found");
        }
        if (services.stream().anyMatch(s -> !s.isActive())) {
            throw new BadRequestException("One or more services are not active");
        }
        return services;
    }

    private int activeStaffCount() {
        return (int) staffRepository.findByActiveTrue().stream()
                .filter(s -> s.getRole() == StaffRole.STYLIST
                        || s.getRole() == StaffRole.THERAPIST)
                .count();
    }

    private int totalDuration(QueueTicket t) {
        return t.getServices().stream()
                .mapToInt(s -> s.getDurationMinutes() + s.getProcessingMinutes()).sum();
    }
}
