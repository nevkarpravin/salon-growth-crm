package com.salon.crm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.salon.crm.config.VirtualRoomProperties;
import com.salon.crm.dto.OutboundMessageResponse;
import com.salon.crm.dto.SimulateRequest;
import com.salon.crm.dto.SimulateResponse;
import com.salon.crm.entity.OutboundMessage;
import com.salon.crm.repository.OutboundMessageRepository;
import com.salon.crm.whatsapp.WhatsAppConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {

    private final WhatsAppConversationService conversationService;
    private final OutboundMessageRepository outboundMessageRepository;
    private final VirtualRoomProperties properties;

    public WhatsAppController(WhatsAppConversationService conversationService,
                              OutboundMessageRepository outboundMessageRepository,
                              VirtualRoomProperties properties) {
        this.conversationService = conversationService;
        this.outboundMessageRepository = outboundMessageRepository;
        this.properties = properties;
    }

    @GetMapping("/webhook")
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode)
                && properties.getWhatsapp().getVerifyToken().equals(token)) {
            return ResponseEntity.ok(challenge != null ? challenge : "");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody JsonNode payload) {
        for (JsonNode entry : payload.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                String profileName = null;
                JsonNode contacts = value.path("contacts");
                if (contacts.isArray() && !contacts.isEmpty()) {
                    profileName = contacts.get(0).path("profile").path("name").asText(null);
                }
                for (JsonNode message : value.path("messages")) {
                    if (!"text".equals(message.path("type").asText())) {
                        continue;
                    }
                    String from = message.path("from").asText(null);
                    String text = message.path("text").path("body").asText(null);
                    if (from != null && text != null) {
                        conversationService.handleInbound(from, text, profileName);
                    }
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulate")
    public SimulateResponse simulate(@Valid @RequestBody SimulateRequest req) {
        Instant since = Instant.now();
        String reply = conversationService.handleInbound(req.from(), req.text(), req.profileName());
        List<OutboundMessageResponse> messages = outboundMessageRepository
                .findByToPhoneOrderByCreatedAtAsc(req.from()).stream()
                .filter(m -> !m.getCreatedAt().isBefore(since))
                .map(this::toResponse)
                .toList();
        return new SimulateResponse(reply, messages);
    }

    @GetMapping("/messages")
    public List<OutboundMessageResponse> messages(@RequestParam(required = false) String phone) {
        List<OutboundMessage> messages = phone != null && !phone.isBlank()
                ? outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc(phone)
                : outboundMessageRepository.findTop100ByOrderByCreatedAtDesc();
        return messages.stream().map(this::toResponse).toList();
    }

    private OutboundMessageResponse toResponse(OutboundMessage m) {
        return new OutboundMessageResponse(m.getId(), m.getToPhone(), m.getBody(), m.getChannel(),
                m.getStatus(), m.getProviderMessageId(), m.getError(),
                m.getTicket() != null ? m.getTicket().getId() : null, m.getCreatedAt());
    }
}
