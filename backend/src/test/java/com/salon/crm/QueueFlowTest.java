package com.salon.crm;

import com.salon.crm.entity.QueueTicket;
import com.salon.crm.entity.QueueTicketStatus;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.entity.Visit;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "crm.seed=false")
@AutoConfigureMockMvc
class QueueFlowTest {

    @Autowired MockMvc mvc;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired QueueTicketRepository queueTicketRepository;
    @Autowired OutboundMessageRepository outboundMessageRepository;
    @Autowired ConversationStateRepository conversationStateRepository;
    @Autowired VisitRepository visitRepository;

    private Staff staff;
    private ServiceItem service;

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

        staff = new Staff();
        staff.setName("Riya");
        staff.setRole(StaffRole.STYLIST);
        staff = staffRepository.save(staff);

        service = new ServiceItem();
        service.setName("Haircut");
        service.setCategory("Hair");
        service.setDurationMinutes(45);
        service.setPrice(BigDecimal.valueOf(800));
        service = serviceItemRepository.save(service);
    }

    @AfterEach
    void cleanQueueTables() {
        outboundMessageRepository.deleteAll();
        conversationStateRepository.deleteAll();
        queueTicketRepository.deleteAll();
    }

    private String joinBody(String phone) {
        return """
                {"phone":"%s","name":"Test","serviceIds":["%s"],"source":"WHATSAPP"}
                """.formatted(phone, service.getId());
    }

    private String joinAndGetId(String phone) throws Exception {
        String body = mvc.perform(post("/api/v1/queue")
                        .contentType(MediaType.APPLICATION_JSON).content(joinBody(phone)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.id");
    }

    @Test
    void joinCreatesTicketAndSendsMessage() throws Exception {
        mvc.perform(post("/api/v1/queue")
                        .contentType(MediaType.APPLICATION_JSON).content(joinBody("7000000001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.etaMinutes").value(0))
                .andExpect(jsonPath("$.publicUrl", containsString("/q/")));

        var messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000001");
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getBody()).contains("Token #1");
    }

    @Test
    void duplicateJoinReturns400() throws Exception {
        joinAndGetId("7000000002");
        mvc.perform(post("/api/v1/queue")
                        .contentType(MediaType.APPLICATION_JSON).content(joinBody("7000000002")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("already in the queue")));
    }

    @Test
    void secondWaitingTicketGetsPositionTwoAndNextNotification() throws Exception {
        joinAndGetId("7000000003");
        mvc.perform(post("/api/v1/queue")
                        .contentType(MediaType.APPLICATION_JSON).content(joinBody("7000000004")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.etaMinutes").value(45));

        var messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000004");
        assertThat(messages.stream().anyMatch(m -> m.getBody().contains("You're next")))
                .isTrue();
    }

    @Test
    void finishCreatesVisitAndPendingBill() throws Exception {
        String id = joinAndGetId("7000000005");
        String clientId = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(get("/api/v1/queue/" + id))
                        .andReturn().getResponse().getContentAsString(), "$.clientId");

        mvc.perform(post("/api/v1/queue/" + id + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_SERVICE"));
        mvc.perform(post("/api/v1/queue/" + id + "/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentLink", startsWith("upi://pay?pa=")))
                .andExpect(jsonPath("$.amount").value(800));

        List<Visit> visits = visitRepository
                .findByClientIdOrderByVisitDateDesc(UUID.fromString(clientId));
        assertThat(visits).hasSize(1);
        assertThat(visits.get(0).getAmount()).isEqualByComparingTo("800");
        assertThat(outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000005")
                .stream().anyMatch(m -> m.getBody().contains("Pay via UPI"))).isTrue();
    }

    @Test
    void markPaidIsIdempotentAndTriggersReview() throws Exception {
        String id = joinAndGetId("7000000006");
        mvc.perform(post("/api/v1/queue/" + id + "/start")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/queue/" + id + "/finish")).andExpect(status().isOk());

        String webhook = "{\"ticketId\":\"" + id
                + "\",\"status\":\"PAID\",\"reference\":\"UTR123\",\"mode\":\"UPI_LINK\"}";
        mvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
        mvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk());

        var messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000006");
        long receipts = messages.stream().filter(m -> m.getBody().contains("Receipt #")).count();
        assertThat(receipts).isEqualTo(1);
        assertThat(messages.stream().anyMatch(m -> m.getBody().contains("rating 1-5"))).isTrue();
    }

    @Test
    void submitReviewStoresRating() throws Exception {
        String id = joinAndGetId("7000000007");
        mvc.perform(post("/api/v1/queue/" + id + "/start")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/queue/" + id + "/finish")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/queue/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Great\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));

        var messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000007");
        assertThat(messages.stream()
                .filter(m -> m.getBody().contains("glad you enjoyed")).count())
                .isEqualTo(1);

        // adding a comment later must not send another thanks message
        mvc.perform(post("/api/v1/queue/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"lovely\"}"))
                .andExpect(status().isOk());
        messages = outboundMessageRepository.findByToPhoneOrderByCreatedAtAsc("7000000007");
        assertThat(messages.stream()
                .filter(m -> m.getBody().contains("glad you enjoyed")
                        || m.getBody().contains("feedback")).count())
                .isEqualTo(1);
    }

    @Test
    void skipMovesBackAndCancelsOnSecondSkip() throws Exception {
        String first = joinAndGetId("7000000010");
        joinAndGetId("7000000011");
        joinAndGetId("7000000012");
        joinAndGetId("7000000013");

        // first is at position 1 with 3 waiting behind; skip -> position 3
        mvc.perform(post("/api/v1/queue/" + first + "/skip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(3))
                .andExpect(jsonPath("$.status").value("WAITING"));

        mvc.perform(post("/api/v1/queue/" + first + "/skip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        QueueTicket t = queueTicketRepository.findById(UUID.fromString(first)).orElseThrow();
        assertThat(t.getCancelReason()).isEqualTo("NO_SHOW");
    }

    @Test
    void publicGetMasksPhone() throws Exception {
        String id = joinAndGetId("7000000020");
        mvc.perform(get("/api/v1/public/queue/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientPhone").value("******0020"));
    }
}
