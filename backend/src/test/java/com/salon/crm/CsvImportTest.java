package com.salon.crm;

import com.salon.crm.entity.Client;
import com.salon.crm.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "crm.seed=false")
@AutoConfigureMockMvc
class CsvImportTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ClientRepository clientRepository;

    @BeforeEach
    void clean() {
        clientRepository.deleteAll();
        Client existing = new Client();
        existing.setFirstName("Existing");
        existing.setPhone("1111111111");
        clientRepository.save(existing);
    }

    @Test
    void importCsv_importsSkipsAndReportsErrors() throws Exception {
        String csv = """
                firstName,lastName,phone,email,dateOfBirth,gender,tags,notes
                Priya,Sharma,2222222222,priya@t.com,1992-05-12,FEMALE,bridal|colour,Note one
                Dup,User,1111111111,,,,,
                ,NoName,3333333333,,,,,
                """;
        MockMultipartFile file = new MockMultipartFile("file", "clients.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/v1/clients/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].row").value(4));

        Client imported = clientRepository.findByPhone("2222222222").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(imported.getTags())
                .containsExactlyInAnyOrder("bridal", "colour");
    }

    @Test
    void templateDownload() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/clients/import/template"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("firstName,lastName,phone")));
    }
}
