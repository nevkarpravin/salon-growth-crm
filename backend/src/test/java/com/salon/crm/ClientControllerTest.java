package com.salon.crm;

import com.salon.crm.entity.Client;
import com.salon.crm.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "crm.seed=false")
@AutoConfigureMockMvc
class ClientControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ClientRepository clientRepository;

    @BeforeEach
    void clean() {
        clientRepository.deleteAll();
    }

    @Test
    void createClient() throws Exception {
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Priya","lastName":"Sharma","phone":"9999900001",
                                 "email":"priya@test.com","gender":"FEMALE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("Priya"))
                .andExpect(jsonPath("$.segments").isArray());
    }

    @Test
    void duplicatePhoneReturns409() throws Exception {
        String body = """
                {"firstName":"Priya","phone":"9999900002"}
                """;
        mvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/clients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Phone"));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"NoName\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    void searchMatchesNameCaseInsensitive() throws Exception {
        Client c = new Client();
        c.setFirstName("Ananya");
        c.setPhone("9999900003");
        clientRepository.save(c);

        mvc.perform(get("/api/v1/clients").param("search", "ananya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Ananya"));

        mvc.perform(get("/api/v1/clients").param("search", "zzzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void segmentFilterVip() throws Exception {
        Client c = new Client();
        c.setFirstName("Big");
        c.setPhone("9999900004");
        clientRepository.save(c);
        // no visits => NEW segment only
        mvc.perform(get("/api/v1/clients").param("segment", "VIP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/clients").param("segment", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void notFoundReturns404() throws Exception {
        mvc.perform(get("/api/v1/clients/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
