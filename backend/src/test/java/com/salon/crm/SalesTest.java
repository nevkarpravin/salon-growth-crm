package com.salon.crm;

import com.salon.crm.entity.Appointment;
import com.salon.crm.entity.AppointmentStatus;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.Product;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.entity.WorkingHours;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ProductRepository;
import com.salon.crm.repository.SaleRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "crm.seed=false")
@AutoConfigureMockMvc
class SalesTest {

    @Autowired MockMvc mvc;
    @Autowired ClientRepository clientRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired SaleRepository saleRepository;
    @Autowired VisitRepository visitRepository;

    private Client client;
    private Staff staff;
    private ServiceItem service;
    private Product product;

    @BeforeEach
    void setup() {
        saleRepository.deleteAll();
        appointmentRepository.deleteAll();
        visitRepository.deleteAll();
        productRepository.deleteAll();
        serviceItemRepository.deleteAll();
        staffRepository.deleteAll();
        clientRepository.deleteAll();

        client = new Client();
        client.setFirstName("Sale");
        client.setPhone("7000000099");
        client = clientRepository.save(client);

        staff = new Staff();
        staff.setName("Riya");
        staff.setRole(StaffRole.STYLIST);
        List<WorkingHours> hours = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            WorkingHours wh = new WorkingHours();
            wh.setDayOfWeek(d);
            wh.setStartTime(LocalTime.of(10, 0));
            wh.setEndTime(LocalTime.of(20, 0));
            hours.add(wh);
        }
        staff.setWorkingHours(hours);
        staff = staffRepository.save(staff);

        service = new ServiceItem();
        service.setName("Haircut");
        service.setDurationMinutes(45);
        service.setPrice(BigDecimal.valueOf(1000));
        service = serviceItemRepository.save(service);

        product = new Product();
        product.setName("Shampoo");
        product.setSku("TST-SHP");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStockQty(10);
        product.setLowStockThreshold(8);
        product = productRepository.save(product);
    }

    private String saleBody(String linesJson) {
        return """
                {"clientId":"%s","staffId":"%s","lines":[%s],"discountAmount":100,"tipAmount":50}
                """.formatted(client.getId(), staff.getId(), linesJson);
    }

    private String line(String type, String refId, int qty, String discount) {
        return """
                {"type":"%s","refId":"%s","quantity":%d,"discountAmount":%s}
                """.formatted(type, refId, qty, discount);
    }

    @Test
    void totalsComputed_serverSide() throws Exception {
        // service 1000 + product 2*500 = 2000 - line disc 100 - order disc 100 = 1800
        // tax 18% of 1800 = 324; total = 1800+324+50 tip = 2174
        String lines = line("SERVICE", service.getId().toString(), 1, "0") + ","
                + line("PRODUCT", product.getId().toString(), 2, "100");
        mvc.perform(post("/api/v1/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(lines)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1900))
                .andExpect(jsonPath("$.taxAmount").value(324.0))
                .andExpect(jsonPath("$.total").value(2174.0))
                .andExpect(jsonPath("$.lines", hasSize(2)));
    }

    @Test
    void pay_insufficientPayment_returns400() throws Exception {
        String body = mvc.perform(post("/api/v1/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(line("SERVICE", service.getId().toString(), 1, "0"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mvc.perform(post("/api/v1/sales/" + id + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CASH\",\"amount\":10}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sales/" + id + "/pay"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pay_decrementsStock_andMarksPaid() throws Exception {
        String lines = line("SERVICE", service.getId().toString(), 1, "0") + ","
                + line("PRODUCT", product.getId().toString(), 2, "0");
        String body = mvc.perform(post("/api/v1/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(lines)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        Double total = com.jayway.jsonpath.JsonPath.read(body, "$.total");

        mvc.perform(post("/api/v1/sales/" + id + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"UPI\",\"amount\":" + total + "}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sales/" + id + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("Low stock"))));

        org.assertj.core.api.Assertions.assertThat(
                productRepository.findById(product.getId()).orElseThrow().getStockQty())
                .isEqualTo(8);
    }

    @Test
    void fromAppointment_pay_completesAppointment_singleVisit() throws Exception {
        LocalDate nextMon = LocalDate.now().plusDays(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (!nextMon.isAfter(LocalDate.now())) nextMon = nextMon.plusWeeks(1);
        String apptBody = """
                {"clientId":"%s","staffId":"%s","startTime":"%s","serviceIds":["%s"]}
                """.formatted(client.getId(), staff.getId(), nextMon.atTime(11, 0), service.getId());
        String apptJson = mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(apptBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String apptId = com.jayway.jsonpath.JsonPath.read(apptJson, "$.id");

        String saleJson = mvc.perform(post("/api/v1/sales/from-appointment/" + apptId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        String saleId = com.jayway.jsonpath.JsonPath.read(saleJson, "$.id");
        Double total = com.jayway.jsonpath.JsonPath.read(saleJson, "$.total");

        mvc.perform(post("/api/v1/sales/" + saleId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\",\"amount\":" + total + "}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sales/" + saleId + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        Appointment appt = appointmentRepository.findById(
                java.util.UUID.fromString(apptId)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(appt.getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
        var visits = visitRepository.findByClientIdOrderByVisitDateDesc(client.getId());
        org.assertj.core.api.Assertions.assertThat(visits).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(visits.get(0).getAmount())
                .isEqualByComparingTo(BigDecimal.valueOf(total));
    }

    @Test
    void editAfterPaid_returns400() throws Exception {
        String body = mvc.perform(post("/api/v1/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(line("SERVICE", service.getId().toString(), 1, "0"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        Double total = com.jayway.jsonpath.JsonPath.read(body, "$.total");
        mvc.perform(post("/api/v1/sales/" + id + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CASH\",\"amount\":" + total + "}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sales/" + id + "/pay")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/sales/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(line("SERVICE", service.getId().toString(), 1, "0"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void voidRestoresStock() throws Exception {
        String lines = line("PRODUCT", product.getId().toString(), 3, "0");
        String body = mvc.perform(post("/api/v1/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(saleBody(lines)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        Double total = com.jayway.jsonpath.JsonPath.read(body, "$.total");
        mvc.perform(post("/api/v1/sales/" + id + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CASH\",\"amount\":" + total + "}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sales/" + id + "/pay")).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(
                productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(7);
        mvc.perform(post("/api/v1/sales/" + id + "/void"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOID"));
        org.assertj.core.api.Assertions.assertThat(
                productRepository.findById(product.getId()).orElseThrow().getStockQty()).isEqualTo(10);
    }
}
