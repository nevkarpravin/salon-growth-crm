package com.salon.crm;

import com.salon.crm.entity.Client;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.entity.WorkingHours;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
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
class SchedulingTest {

    @Autowired MockMvc mvc;
    @Autowired ClientRepository clientRepository;
    @Autowired StaffRepository staffRepository;
    @Autowired ServiceItemRepository serviceItemRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired SaleRepository saleRepository;
    @Autowired VisitRepository visitRepository;

    private Client client;
    private Staff staff;
    private ServiceItem service;
    private LocalDate nextMonday;

    @BeforeEach
    void setup() {
        saleRepository.deleteAll();
        appointmentRepository.deleteAll();
        visitRepository.deleteAll();
        staffRepository.deleteAll();
        serviceItemRepository.deleteAll();
        clientRepository.deleteAll();

        client = new Client();
        client.setFirstName("Test");
        client.setPhone("7000000001");
        client = clientRepository.save(client);

        staff = new Staff();
        staff.setName("Riya");
        staff.setRole(StaffRole.STYLIST);
        staff.setColorHex("#ff0000");
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
        service.setCategory("Hair");
        service.setDurationMinutes(60);
        service.setPrice(BigDecimal.valueOf(800));
        service = serviceItemRepository.save(service);

        nextMonday = LocalDate.now().plusDays(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (!nextMonday.isAfter(LocalDate.now())) {
            nextMonday = nextMonday.plusWeeks(1);
        }
    }

    private String appointmentBody(LocalDateTime start) {
        return """
                {"clientId":"%s","staffId":"%s","startTime":"%s","serviceIds":["%s"]}
                """.formatted(client.getId(), staff.getId(), start, service.getId());
    }

    @Test
    void createAppointment_conflictReturns409() throws Exception {
        LocalDateTime start = nextMonday.atTime(11, 0);
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endTime").value(start.plusMinutes(60).withNano(0).toString() + ":00"))
                .andExpect(jsonPath("$.totalPrice").value(800));
        // overlapping 30 min in
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentBody(start.plusMinutes(30))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Appointment Conflict"));
    }

    @Test
    void createAppointment_outsideHoursReturns400() throws Exception {
        LocalDateTime start = nextMonday.atTime(8, 0); // before 10:00
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(start)))
                .andExpect(status().isBadRequest());
        // ends after 20:00
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appointmentBody(nextMonday.atTime(19, 30))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void availability_excludesBookedSlots() throws Exception {
        LocalDateTime start = nextMonday.atTime(12, 0);
        mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(start)))
                .andExpect(status().isCreated());

        String response = mvc.perform(get("/api/v1/appointments/availability")
                        .param("staffId", staff.getId().toString())
                        .param("date", nextMonday.toString())
                        .param("serviceIds", service.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", not(empty())))
                .andReturn().getResponse().getContentAsString();

        // no slot may overlap [12:00, 13:00)
        org.assertj.core.api.Assertions.assertThat(response)
                .doesNotContain("\"start\":\"" + nextMonday + "T11:15")
                .doesNotContain("\"start\":\"" + nextMonday + "T11:30")
                .doesNotContain("\"start\":\"" + nextMonday + "T11:45")
                .doesNotContain("\"start\":\"" + nextMonday + "T12:00")
                .doesNotContain("\"start\":\"" + nextMonday + "T12:15")
                .doesNotContain("\"start\":\"" + nextMonday + "T12:30")
                .doesNotContain("\"start\":\"" + nextMonday + "T12:45");
        org.assertj.core.api.Assertions.assertThat(response)
                .contains("\"start\":\"" + nextMonday + "T13:00");
    }

    @Test
    void completingAppointment_createsVisit() throws Exception {
        LocalDateTime start = nextMonday.atTime(14, 0);
        String body = mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(start)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        var visits = visitRepository.findByClientIdOrderByVisitDateDesc(client.getId());
        org.assertj.core.api.Assertions.assertThat(visits).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(visits.get(0).getStylistName()).isEqualTo("Riya");
        org.assertj.core.api.Assertions.assertThat(visits.get(0).getServices()).containsExactly("Haircut");
        org.assertj.core.api.Assertions.assertThat(visits.get(0).getAmount())
                .isEqualByComparingTo("800");
    }

    @Test
    void invalidTransition_returns400() throws Exception {
        LocalDateTime start = nextMonday.atTime(15, 0);
        String body = mvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(start)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/appointments/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
    }
}
