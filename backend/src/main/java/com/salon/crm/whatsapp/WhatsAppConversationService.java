package com.salon.crm.whatsapp;

import com.salon.crm.config.VirtualRoomProperties;
import com.salon.crm.dto.JoinQueueRequest;
import com.salon.crm.dto.QueueTicketResponse;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.ConversationState;
import com.salon.crm.entity.ConversationStep;
import com.salon.crm.entity.PaymentStatus;
import com.salon.crm.entity.PreferredChannel;
import com.salon.crm.entity.QueueTicket;
import com.salon.crm.entity.QueueTicketStatus;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ConversationStateRepository;
import com.salon.crm.repository.QueueTicketRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.service.QueueService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class WhatsAppConversationService {

    private static final Set<QueueTicketStatus> ACTIVE =
            EnumSet.of(QueueTicketStatus.WAITING, QueueTicketStatus.CALLED,
                    QueueTicketStatus.IN_SERVICE);
    private static final Set<String> MENU_WORDS =
            Set.of("menu", "hi", "hello", "0", "start", "help");

    private final ConversationStateRepository conversationStateRepository;
    private final ClientRepository clientRepository;
    private final QueueTicketRepository ticketRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final StaffRepository staffRepository;
    private final QueueService queueService;
    private final MessagingService messagingService;
    private final VirtualRoomProperties properties;

    public WhatsAppConversationService(ConversationStateRepository conversationStateRepository,
                                       ClientRepository clientRepository,
                                       QueueTicketRepository ticketRepository,
                                       ServiceItemRepository serviceItemRepository,
                                       StaffRepository staffRepository,
                                       QueueService queueService,
                                       MessagingService messagingService,
                                       VirtualRoomProperties properties) {
        this.conversationStateRepository = conversationStateRepository;
        this.clientRepository = clientRepository;
        this.ticketRepository = ticketRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.staffRepository = staffRepository;
        this.queueService = queueService;
        this.messagingService = messagingService;
        this.properties = properties;
    }

    public String handleInbound(String phone, String text, String profileName) {
        String normalized = text != null ? text.trim().toLowerCase() : "";
        ConversationState state = conversationStateRepository.findByPhone(phone)
                .orElseGet(() -> {
                    ConversationState s = new ConversationState();
                    s.setPhone(phone);
                    s.setStep(ConversationStep.MENU);
                    return s;
                });

        String reply = dispatch(phone, normalized, profileName, state);
        conversationStateRepository.save(state);
        messagingService.send(phone, reply, null);
        return reply;
    }

    private String dispatch(String phone, String text, String profileName,
                            ConversationState state) {
        Client client = clientRepository.findByPhone(phone).orElse(null);
        if (client == null) {
            client = new Client();
            client.setPhone(phone);
            client.setWhatsappOptIn(true);
            client.setPreferredChannel(PreferredChannel.WHATSAPP);
            client.setMarketingConsentAt(Instant.now());
            if (profileName != null && !profileName.isBlank()) {
                client.setFirstName(profileName.trim());
                client = clientRepository.save(client);
                state.setStep(ConversationStep.MENU);
                return menu(client);
            }
            client.setFirstName("Guest");
            client = clientRepository.save(client);
            state.setStep(ConversationStep.ASK_NAME);
            return "Welcome to " + properties.getSalonName() + "! What's your name?";
        }

        if (state.getStep() == ConversationStep.ASK_NAME) {
            if (MENU_WORDS.contains(text)) {
                state.setStep(ConversationStep.MENU);
                return menu(client);
            }
            client.setFirstName(capitalize(text));
            clientRepository.save(client);
            state.setStep(ConversationStep.MENU);
            return menu(client);
        }

        // "0" only means "back to menu" at the menu itself; mid-flow it is a
        // valid answer (0 = any staff, 0 = skip comment).
        boolean resetToMenu = MENU_WORDS.contains(text)
                && (!text.equals("0") || state.getStep() == ConversationStep.MENU);
        if (resetToMenu) {
            state.setStep(ConversationStep.MENU);
            state.getDraftServiceIds().clear();
            state.setDraftStaffId(null);
            return menu(client);
        }

        return switch (state.getStep()) {
            case CHOOSE_SERVICES -> chooseServices(phone, text, client, state);
            case CHOOSE_STAFF -> chooseStaff(phone, text, client, state);
            case CONFIRM_PRESENCE -> confirmPresence(text, state);
            case REVIEW_RATING -> reviewRating(text, state);
            case REVIEW_COMMENT -> reviewComment(text, state);
            case MENU -> menuChoice(phone, text, client, state);
            default -> menu(client);
        };
    }

    private String menuChoice(String phone, String text, Client client,
                              ConversationState state) {
        return switch (text) {
            case "1" -> startJoin(client, state);
            case "2" -> queueStatus(phone);
            case "3" -> payBill(phone);
            case "4" -> startReview(phone, state);
            case "5" -> leaveQueue(phone, state);
            default -> menu(client);
        };
    }

    private String startJoin(Client client, ConversationState state) {
        QueueTicket active = activeTicket(client.getPhone());
        if (active != null) {
            int[] pos = queueService.positionAndEta(active);
            return "You're already in the queue (token #" + active.getTokenNumber()
                    + ", position " + pos[0] + ", ~" + pos[1] + " min). "
                    + "Reply 2 to check or 5 to leave.";
        }
        List<ServiceItem> services = menuServices();
        if (services.isEmpty()) {
            return "Sorry, no services are available right now.";
        }
        StringBuilder sb = new StringBuilder("What would you like?\n");
        for (int i = 0; i < services.size(); i++) {
            ServiceItem s = services.get(i);
            sb.append(i + 1).append(" ").append(s.getName())
                    .append(" — ").append(s.getDurationMinutes()).append(" min — ₹")
                    .append(MessageTemplates.amountText(s.getPrice())).append("\n");
        }
        sb.append("Reply with the numbers you want, comma separated (e.g. 1,3)");
        state.setStep(ConversationStep.CHOOSE_SERVICES);
        state.getDraftServiceIds().clear();
        return sb.toString();
    }

    private String chooseServices(String phone, String text, Client client,
                                  ConversationState state) {
        List<ServiceItem> services = menuServices();
        List<Integer> picks = parseNumbers(text);
        if (picks.isEmpty() || picks.stream().anyMatch(n -> n < 1 || n > services.size())) {
            return "Please reply with valid numbers, e.g. 1,3";
        }
        List<UUID> ids = picks.stream().distinct().map(n -> services.get(n - 1).getId()).toList();
        state.setDraftServiceIds(new ArrayList<>(ids));

        List<Staff> staff = menuStaff();
        StringBuilder sb = new StringBuilder("Who would you like?\n0 Any available\n");
        for (int i = 0; i < staff.size(); i++) {
            sb.append(i + 1).append(" ").append(staff.get(i).getName()).append("\n");
        }
        state.setStep(ConversationStep.CHOOSE_STAFF);
        return sb.toString().trim();
    }

    private String chooseStaff(String phone, String text, Client client,
                               ConversationState state) {
        List<Staff> staff = menuStaff();
        List<Integer> picks = parseNumbers(text);
        if (picks.size() != 1 || picks.get(0) < 0 || picks.get(0) > staff.size()) {
            return "Please reply with a number, e.g. 0 for any available.";
        }
        UUID staffId = picks.get(0) == 0 ? null : staff.get(picks.get(0) - 1).getId();
        state.setDraftStaffId(staffId);
        state.setStep(ConversationStep.MENU);

        try {
            QueueTicketResponse ticket = queueService.join(new JoinQueueRequest(
                    phone, client.getFirstName(), state.getDraftServiceIds(),
                    staffId, "WHATSAPP"), false);
            QueueTicket entity = queueService.find(ticket.id());
            String reply = MessageTemplates.joinConfirmation(properties.getSalonName(),
                    client.getFirstName(), entity,
                    ticket.peopleAhead() != null ? ticket.peopleAhead() : 0,
                    ticket.etaMinutes() != null ? ticket.etaMinutes() : 0,
                    properties.getPublicBaseUrl());
            state.setActiveTicketId(ticket.id());
            return reply;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    private String queueStatus(String phone) {
        QueueTicket t = activeTicket(phone);
        if (t == null) {
            QueueTicket recent = latestToday(phone);
            if (recent != null && recent.getStatus() == QueueTicketStatus.COMPLETED) {
                return "Token #" + recent.getTokenNumber() + ": done. "
                        + (recent.getPaymentStatus() == PaymentStatus.PAID
                        ? "Paid — thank you!" : "Your bill is pending. Reply 3 to pay.");
            }
            return "You're not in the queue. Reply 1 to join.";
        }
        if (t.getStatus() == QueueTicketStatus.IN_SERVICE) {
            return "Token #" + t.getTokenNumber() + ": your service is in progress.";
        }
        int[] pos = queueService.positionAndEta(t);
        List<QueueTicket> queued = ticketRepository
                .findByQueueDateAndStatusInOrderByQueueOrderAsc(t.getQueueDate(),
                        Set.of(QueueTicketStatus.WAITING, QueueTicketStatus.CALLED));
        return "Token #" + t.getTokenNumber() + ": "
                + (t.getStatus() == QueueTicketStatus.CALLED ? "called — " : "")
                + "position " + pos[0] + " of " + queued.size()
                + ", ~" + pos[1] + " min wait. Track: "
                + properties.getPublicBaseUrl() + "/q/" + t.getId();
    }

    private String payBill(String phone) {
        QueueTicket t = latestToday(phone);
        if (t == null || t.getStatus() != QueueTicketStatus.COMPLETED) {
            return "No pending bill.";
        }
        if (t.getPaymentStatus() == PaymentStatus.PAID) {
            return "Already paid, thank you!";
        }
        return MessageTemplates.bill(t, t.getPaymentLink() != null ? t.getPaymentLink()
                : "");
    }

    private String startReview(String phone, ConversationState state) {
        QueueTicket t = latestToday(phone);
        if (t == null || t.getStatus() != QueueTicketStatus.COMPLETED) {
            return "Nothing to review yet.";
        }
        if (t.getRating() != null) {
            return "You've already rated this visit.";
        }
        state.setStep(ConversationStep.REVIEW_RATING);
        state.setActiveTicketId(t.getId());
        return "Rate your visit 1-5";
    }

    private String leaveQueue(String phone, ConversationState state) {
        QueueTicket t = activeTicket(phone);
        if (t == null || (t.getStatus() != QueueTicketStatus.WAITING
                && t.getStatus() != QueueTicketStatus.CALLED)) {
            return "You're not in the queue.";
        }
        queueService.leave(t.getId());
        state.setActiveTicketId(null);
        return "You've left the queue. See you soon!";
    }

    private String confirmPresence(String text, ConversationState state) {
        UUID ticketId = state.getActiveTicketId();
        switch (text) {
            case "1" -> {
                state.setStep(ConversationStep.MENU);
                return "Great, see you at the counter!";
            }
            case "2" -> {
                state.setStep(ConversationStep.MENU);
                if (ticketId != null) {
                    queueService.delay(ticketId);
                }
                return "No problem — we've moved you back a few places.";
            }
            case "3" -> {
                state.setStep(ConversationStep.MENU);
                state.setActiveTicketId(null);
                if (ticketId != null) {
                    queueService.leave(ticketId);
                }
                return "You've left the queue. See you soon!";
            }
            default -> {
                return "Please reply 1 = I'm here, 2 = need 10 more minutes, 3 = cancel";
            }
        }
    }

    private String reviewRating(String text, ConversationState state) {
        Integer rating = parseNumbers(text).stream().findFirst().orElse(null);
        if (rating == null || rating < 1 || rating > 5) {
            return "Please reply 1-5";
        }
        if (state.getActiveTicketId() != null) {
            queueService.submitReview(state.getActiveTicketId(), rating, null);
        }
        state.setStep(ConversationStep.REVIEW_COMMENT);
        return "Thanks! Any comment? Reply with text or 0 to skip.";
    }

    private String reviewComment(String text, ConversationState state) {
        state.setStep(ConversationStep.MENU);
        if (text.equals("0") || text.equals("skip")) {
            return thankYou(state);
        }
        if (state.getActiveTicketId() != null) {
            QueueTicket t = queueService.find(state.getActiveTicketId());
            int rating = t.getRating() != null ? t.getRating() : 5;
            queueService.submitReview(t.getId(), rating, text);
        }
        return thankYou(state);
    }

    private String thankYou(ConversationState state) {
        state.setActiveTicketId(null);
        return "Thank you for your feedback!";
    }

    // ---- helpers ----

    private String menu(Client client) {
        return MessageTemplates.menu(properties.getSalonName(), client.getFirstName());
    }

    private QueueTicket activeTicket(String phone) {
        return ticketRepository
                .findFirstByClient_PhoneAndStatusInOrderByJoinedAtDesc(phone, ACTIVE)
                .filter(t -> java.time.LocalDate.now().equals(t.getQueueDate()))
                .orElse(null);
    }

    private QueueTicket latestToday(String phone) {
        return ticketRepository
                .findByQueueDateOrderByQueueOrderAsc(java.time.LocalDate.now()).stream()
                .filter(t -> t.getClient().getPhone().equals(phone))
                .reduce((a, b) -> b)
                .orElse(null);
    }

    private List<ServiceItem> menuServices() {
        return serviceItemRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(ServiceItem::getCategory,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(ServiceItem::getName))
                .toList();
    }

    private List<Staff> menuStaff() {
        return staffRepository.findByActiveTrue().stream()
                .filter(s -> s.getRole() == StaffRole.STYLIST || s.getRole() == StaffRole.THERAPIST)
                .sorted(Comparator.comparing(Staff::getName))
                .toList();
    }

    private List<Integer> parseNumbers(String text) {
        List<Integer> nums = new ArrayList<>();
        for (String part : text.split("[,\\s]+")) {
            if (part.isBlank()) continue;
            try {
                nums.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }
        return nums;
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return "Guest";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
