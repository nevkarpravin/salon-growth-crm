package com.salon.crm;

import com.salon.crm.entity.QueueTicket;
import com.salon.crm.entity.QueueTicketStatus;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ConversationStateRepository;
import com.salon.crm.repository.OutboundMessageRepository;
import com.salon.crm.repository.QueueTicketRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.VisitRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "crm.seed=false")
@AutoConfigureMockMvc
class WhatsAppConversationTest {

    @Autowired MockMvc mvc;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired VisitRepository visitRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired QueueTicketRepository queueTicketRepository;
    @Autowired OutboundMessageRepository outboundMessageRepository;
    @Autowired ConversationStateRepository conversationStateRepository;

    private static final String PHONE = "9111100000";

    @BeforeEach
    void setup() {
        outboundMessageRepository.deleteAll();
        conversationStateRepository.deleteAll();
        queueTicketRepository.deleteAll();
        appointmentRepository.deleteAll();
        visitRepository.deleteAll();
        staffRepository.deleteAll();
        serviceItemRepository.deleteAll();
        clientRepository.deleteAll();

        Staff staff = new Staff();
        staff.setName("Riya Kapoor");
        staff.setRole(StaffRole.STYLIST);
        staffRepository.save(staff);

        ServiceItem service = new ServiceItem();
        service.setName("Haircut");
        service.setCategory("Hair");
        service.setDurationMinutes(45);
        service.setPrice(BigDecimal.valueOf(800));
        serviceItemRepository.save(service);
    }

    @AfterEach
    void cleanQueueTables() {
        outboundMessageRepository.deleteAll();
        conversationStateRepository.deleteAll();
        queueTicketRepository.deleteAll();
    }

    private String simulate(String phone, String text, String profileName) throws Exception {
        String body = profileName != null
                ? "{\"from\":\"" + phone + "\",\"text\":\"" + text
                + "\",\"profileName\":\"" + profileName + "\"}"
                : "{\"from\":\"" + phone + "\",\"text\":\"" + text + "\"}";
        return mvc.perform(post("/api/v1/whatsapp/simulate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void hiFromUnknownPhoneShowsMenu() throws Exception {
        String reply = simulate(PHONE, "Hi", "Test User");
        assertThat(reply).contains("1 Join queue");
        assertThat(clientRepository.findByPhone(PHONE)).isPresent();
    }

    @Test
    void fullJoinFlow() throws Exception {
        simulate(PHONE, "Hi", "Test User");
        String r1 = simulate(PHONE, "1", null);
        assertThat(r1).contains("Haircut");
        String r2 = simulate(PHONE, "1", null);
        assertThat(r2).contains("0 Any available").contains("Riya Kapoor");
        String r3 = simulate(PHONE, "0", null);
        assertThat(r3).contains("Token #");
        assertThat(queueTicketRepository
                .findFirstByClient_PhoneAndStatusInOrderByJoinedAtDesc(PHONE,
                        java.util.Set.of(QueueTicketStatus.WAITING)))
                .isPresent();
    }

    @Test
    void statusAndLeaveFlow() throws Exception {
        simulate(PHONE, "Hi", "Test User");
        simulate(PHONE, "1", null);
        simulate(PHONE, "1", null);
        simulate(PHONE, "0", null);

        String status = simulate(PHONE, "2", null);
        assertThat(status).contains("position 1");

        String left = simulate(PHONE, "5", null);
        assertThat(left).contains("left the queue");
        QueueTicket t = queueTicketRepository.findByQueueDateOrderByQueueOrderAsc(
                LocalDate.now()).get(0);
        assertThat(t.getStatus()).isEqualTo(QueueTicketStatus.CANCELLED);
    }

    @Test
    void reviewFlowSendsSingleThanks() throws Exception {
        simulate(PHONE, "Hi", "Test User");
        simulate(PHONE, "1", null);
        simulate(PHONE, "1", null);
        simulate(PHONE, "0", null);

        QueueTicket t = queueTicketRepository.findByQueueDateOrderByQueueOrderAsc(
                LocalDate.now()).get(0);
        mvc.perform(post("/api/v1/queue/" + t.getId() + "/start"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/queue/" + t.getId() + "/finish"))
                .andExpect(status().isOk());

        String prompt = simulate(PHONE, "4", null);
        assertThat(prompt).contains("1-5");
        String askComment = simulate(PHONE, "5", null);
        assertThat(askComment).contains("Any comment");
        String thanks = simulate(PHONE, "Lovely cut", null);
        assertThat(thanks).contains("Thank you");

        var messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc(PHONE);
        // exactly one thanks message from the service across rating + comment
        assertThat(messages.stream()
                .filter(m -> m.getBody().contains("glad you enjoyed")).count())
                .isEqualTo(1);
    }

    @Test
    void metaWebhookProcessesTextMessage() throws Exception {
        String payload = """
                {"entry":[{"changes":[{"value":{
                  "contacts":[{"profile":{"name":"Meta User"}}],
                  "messages":[{"from":"9222200000","type":"text","text":{"body":"Hi"}}]
                }}]}]}
                """;
        mvc.perform(post("/api/v1/whatsapp/webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        assertThat(clientRepository.findByPhone("9222200000")).isPresent();
    }

    @Test
    void webhookVerification() throws Exception {
        mvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "salon-verify")
                        .param("hub.challenge", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string("abc123"));
        mvc.perform(get("/api/v1/whatsapp/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "wrong")
                        .param("hub.challenge", "abc123"))
                .andExpect(status().isForbidden());
    }
}
