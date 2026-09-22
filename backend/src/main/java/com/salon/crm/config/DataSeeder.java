package com.salon.crm.config;

import com.salon.crm.entity.Client;
import com.salon.crm.entity.FormulaCard;
import com.salon.crm.entity.Gender;
import com.salon.crm.entity.PreferredChannel;
import com.salon.crm.entity.Visit;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.FormulaCardRepository;
import com.salon.crm.repository.VisitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "crm.seed", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final FormulaCardRepository formulaCardRepository;

    public DataSeeder(ClientRepository clientRepository, VisitRepository visitRepository,
                      FormulaCardRepository formulaCardRepository) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.formulaCardRepository = formulaCardRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (clientRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        seed(today);
    }

    private void seed(LocalDate today) {
        record SeedData(String first, String last, String phone, String email, LocalDate dob,
                        Gender gender, Set<String> tags, int[][] visits, String[] formulas,
                        PreferredChannel channel) {
        }

        List<SeedData> data = List.of(
                new SeedData("Priya", "Sharma", "9876543210", "priya.sharma@gmail.com",
                        LocalDate.of(1992, today.getMonthValue(), 12), Gender.FEMALE,
                        Set.of("bridal", "hair-colour"),
                        new int[][]{{3, 2500, 0}, {45, 1800, 0}, {120, 3200, 0}},
                        new String[]{"Global Colour|L'Oréal Majirel 6.34 + 20vol", "Balayage|Blondor + Olaplex"},
                        PreferredChannel.WHATSAPP),
                new SeedData("Ananya", "Iyer", "9876501234", "ananya.iyer@yahoo.in",
                        LocalDate.of(1988, 3, 22), Gender.FEMALE, Set.of("regular"),
                        new int[][]{{10, 800, 0}, {40, 1200, 0}},
                        new String[]{"Keratin|GK Hair treatment"}, PreferredChannel.SMS),
                new SeedData("Rohan", "Mehta", "9822011223", "rohan.mehta@outlook.com",
                        LocalDate.of(1995, 7, 8), Gender.MALE, Set.of("beard"),
                        new int[][]{{5, 500, 0}}, new String[]{}, PreferredChannel.NONE),
                new SeedData("Sneha", "Kulkarni", "9812345678", null,
                        LocalDate.of(2001, today.getMonthValue(), 5), Gender.FEMALE,
                        Set.of("student", "nails"), new int[][]{{7, 1500, 0}},
                        new String[]{"Nail art|Gel extension"}, PreferredChannel.WHATSAPP),
                new SeedData("Vikram", "Desai", "9898901234", "vdesai@gmail.com",
                        LocalDate.of(1980, 11, 30), Gender.MALE, Set.of("regular"),
                        new int[][]{{50, 900, 0}, {80, 900, 0}, {140, 1100, 0}},
                        new String[]{}, PreferredChannel.SMS),
                new SeedData("Meera", "Nair", "9765432109", "meera.nair@gmail.com",
                        LocalDate.of(1990, 1, 15), Gender.FEMALE, Set.of("vip", "facial"),
                        new int[][]{{2, 4500, 0}, {20, 5000, 0}, {55, 3800, 0}, {90, 6200, 0},
                                {130, 4000, 0}, {170, 5500, 0}, {200, 4800, 0}, {230, 5200, 0},
                                {260, 3900, 0}, {290, 6100, 0}, {320, 4700, 0}},
                        new String[]{"Facial|O3+ whitening", "Spa|Aroma therapy"},
                        PreferredChannel.EMAIL),
                new SeedData("Arjun", "Patel", "9654321098", null,
                        LocalDate.of(1998, 9, 2), Gender.MALE, Set.of("new"),
                        new int[][]{{1, 700, 0}}, new String[]{}, PreferredChannel.WHATSAPP),
                new SeedData("Kavita", "Reddy", "9543210987", "kavita.reddy@gmail.com",
                        LocalDate.of(1985, 6, 18), Gender.FEMALE, Set.of("hair-spa"),
                        new int[][]{{47, 2200, 0}, {100, 2500, 0}},
                        new String[]{"Hair Spa|Schwarzkopf BC"}, PreferredChannel.SMS),
                new SeedData("Deepika", "Joshi", "9432109876", "deepika.j@gmail.com",
                        LocalDate.of(1993, 12, 1), Gender.FEMALE, Set.of("bridal"),
                        new int[][]{{70, 15000, 0}, {200, 8000, 0}},
                        new String[]{"Bridal|HD makeup base"}, PreferredChannel.WHATSAPP),
                new SeedData("Amit", "Verma", "9321098765", null,
                        LocalDate.of(1978, 4, 25), Gender.MALE, Set.of("lapsed"),
                        new int[][]{{95, 600, 0}}, new String[]{}, PreferredChannel.NONE),
                new SeedData("Pooja", "Singh", "9210987654", "pooja.singh@gmail.com",
                        LocalDate.of(1997, today.getMonthValue(), 20), Gender.FEMALE,
                        Set.of("colour", "regular"),
                        new int[][]{{15, 3500, 0}, {60, 2800, 0}},
                        new String[]{"Highlights|Wella Koleston"}, PreferredChannel.EMAIL),
                new SeedData("Karan", "Malhotra", "9109876543", "karan.m@gmail.com",
                        LocalDate.of(1991, 8, 9), Gender.MALE, Set.of("regular"),
                        new int[][]{{25, 950, 0}}, new String[]{}, PreferredChannel.SMS),
                new SeedData("Lakshmi", "Menon", "9098765432", null,
                        LocalDate.of(1983, 2, 14), Gender.FEMALE, Set.of("vip", "facial"),
                        new int[][]{{12, 6000, 0}, {35, 7200, 0}, {75, 5400, 0}, {110, 6800, 0},
                                {150, 4500, 0}, {180, 7300, 0}, {215, 5900, 0}, {250, 6600, 0},
                                {280, 5100, 0}, {310, 7000, 0}},
                        new String[]{"Facial|Gold facial kit"}, PreferredChannel.WHATSAPP),
                new SeedData("Nisha", "Agarwal", "8987654321", "nisha.a@hotmail.com",
                        LocalDate.of(2000, 10, 30), Gender.FEMALE, Set.of("student"),
                        new int[][]{{20, 1300, 0}}, new String[]{}, PreferredChannel.NONE),
                new SeedData("Rahul", "Khanna", "8876543210", null,
                        LocalDate.of(1987, 5, 6), Gender.MALE, Set.of("at-risk"),
                        new int[][]{{52, 1100, 0}, {130, 1400, 0}},
                        new String[]{}, PreferredChannel.SMS));

        for (SeedData s : data) {
            Client c = new Client();
            c.setFirstName(s.first());
            c.setLastName(s.last());
            c.setPhone(s.phone());
            c.setEmail(s.email());
            c.setDateOfBirth(s.dob());
            c.setGender(s.gender());
            c.setTags(s.tags());
            c.setPreferredChannel(s.channel());
            c.setWhatsappOptIn(s.channel() == PreferredChannel.WHATSAPP);
            c.setSmsOptIn(s.channel() == PreferredChannel.SMS);
            c.setEmailOptIn(s.channel() == PreferredChannel.EMAIL);
            clientRepository.save(c);

            for (int[] v : s.visits()) {
                Visit visit = new Visit();
                visit.setClient(c);
                visit.setVisitDate(today.minusDays(v[0]));
                visit.setStylistName(List.of("Riya", "Sameer", "Farhan", "Anjali").get(v[0] % 4));
                visit.setServices(List.of("Haircut", "Styling"));
                visit.setAmount(BigDecimal.valueOf(v[1]));
                visitRepository.save(visit);
            }
            for (String f : s.formulas()) {
                String[] parts = f.split("\\|", 2);
                FormulaCard card = new FormulaCard();
                card.setClient(c);
                card.setServiceName(parts[0]);
                card.setFormula(parts.length > 1 ? parts[1] : "");
                card.setRecordedBy("Riya");
                card.setRecordedAt(today.minusDays(30));
                formulaCardRepository.save(card);
            }
        }
    }
}
