package com.salon.crm.service;

import com.salon.crm.dto.ImportResult;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.Gender;
import com.salon.crm.repository.ClientRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CsvImportService {

    public static final String TEMPLATE =
            "firstName,lastName,phone,email,dateOfBirth,gender,tags,notes\n";

    private final ClientRepository clientRepository;

    public CsvImportService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) throws IOException {
        int imported = 0;
        int skipped = 0;
        List<ImportResult.RowError> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = new CSVParser(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), format)) {
            for (CSVRecord record : parser) {
                int row = (int) record.getRecordNumber() + 1; // +1 for header row
                try {
                    String phone = get(record, "phone");
                    if (phone == null || phone.isBlank()) {
                        errors.add(new ImportResult.RowError(row, "phone is required"));
                        continue;
                    }
                    if (clientRepository.existsByPhone(phone)) {
                        skipped++;
                        continue;
                    }
                    String firstName = get(record, "firstName");
                    if (firstName == null || firstName.isBlank()) {
                        errors.add(new ImportResult.RowError(row, "firstName is required"));
                        continue;
                    }
                    Client client = new Client();
                    client.setFirstName(firstName);
                    client.setLastName(get(record, "lastName"));
                    client.setPhone(phone);
                    client.setEmail(get(record, "email"));
                    String dob = get(record, "dateOfBirth");
                    if (dob != null && !dob.isBlank()) {
                        client.setDateOfBirth(LocalDate.parse(dob));
                    }
                    String gender = get(record, "gender");
                    if (gender != null && !gender.isBlank()) {
                        client.setGender(Gender.valueOf(gender.trim().toUpperCase()));
                    }
                    String tags = get(record, "tags");
                    if (tags != null && !tags.isBlank()) {
                        Set<String> tagSet = new LinkedHashSet<>();
                        for (String t : tags.split("\\|")) {
                            if (!t.isBlank()) {
                                tagSet.add(t.trim());
                            }
                        }
                        client.setTags(tagSet);
                    }
                    client.setNotes(get(record, "notes"));
                    clientRepository.save(client);
                    imported++;
                } catch (IllegalArgumentException e) {
                    errors.add(new ImportResult.RowError(row, "Invalid value: " + e.getMessage()));
                }
            }
        }
        return new ImportResult(imported, skipped, errors);
    }

    private String get(CSVRecord record, String column) {
        try {
            return record.get(column);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
