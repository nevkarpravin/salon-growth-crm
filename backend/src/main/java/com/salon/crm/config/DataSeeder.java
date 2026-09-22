package com.salon.crm.config;

import com.salon.crm.entity.Appointment;
import com.salon.crm.entity.AppointmentStatus;
import com.salon.crm.entity.AppointmentSource;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.FormulaCard;
import com.salon.crm.entity.Gender;
import com.salon.crm.entity.PreferredChannel;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.StaffRole;
import com.salon.crm.entity.Visit;
import com.salon.crm.entity.WorkingHours;
import com.salon.crm.entity.Payment;
import com.salon.crm.entity.PaymentMethod;
import com.salon.crm.entity.Product;
import com.salon.crm.entity.Sale;
import com.salon.crm.entity.SaleLine;
import com.salon.crm.entity.SaleLineType;
import com.salon.crm.entity.SaleStatus;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.FormulaCardRepository;
import com.salon.crm.repository.ProductRepository;
import com.salon.crm.repository.SaleRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.VisitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "crm.seed", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;
    private final FormulaCardRepository formulaCardRepository;
    private final StaffRepository staffRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    public DataSeeder(ClientRepository clientRepository, VisitRepository visitRepository,
                      FormulaCardRepository formulaCardRepository,
                      StaffRepository staffRepository,
                      ServiceItemRepository serviceItemRepository,
                      AppointmentRepository appointmentRepository,
                      ProductRepository productRepository,
                      SaleRepository saleRepository) {
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
        this.formulaCardRepository = formulaCardRepository;
        this.staffRepository = staffRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.appointmentRepository = appointmentRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
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

        seedScheduling(today);
    }

    private void seedScheduling(LocalDate today) {
        // Staff: 4 members, Mon–Sat 10:00–20:00
        String[][] staffData = {
                {"Riya Kapoor", "STYLIST", "#e11d48"},
                {"Sameer Sheikh", "STYLIST", "#7c3aed"},
                {"Farhan Ali", "THERAPIST", "#059669"},
                {"Anjali Deshmukh", "RECEPTIONIST", "#d97706"},
        };
        List<WorkingHours> monSat = new ArrayList<>();
        for (DayOfWeek d : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            WorkingHours wh = new WorkingHours();
            wh.setDayOfWeek(d);
            wh.setStartTime(LocalTime.of(10, 0));
            wh.setEndTime(LocalTime.of(20, 0));
            monSat.add(wh);
        }
        List<Staff> staffList = new ArrayList<>();
        for (String[] sd : staffData) {
            Staff staff = new Staff();
            staff.setName(sd[0]);
            staff.setRole(StaffRole.valueOf(sd[1]));
            staff.setColorHex(sd[2]);
            staff.setPhone("98" + (10000000 + staffList.size() * 111111));
            staff.setWorkingHours(new ArrayList<>(monSat));
            staffList.add(staffRepository.save(staff));
        }

        // Services
        Object[][] serviceData = {
                {"Haircut", "Hair", 45, 0, 800},
                {"Hair Colour", "Hair", 90, 30, 3500},
                {"Balayage", "Hair", 120, 45, 5500},
                {"Hair Spa", "Hair", 60, 0, 1500},
                {"Keratin Treatment", "Hair", 120, 0, 4000},
                {"Facial", "Skin", 60, 0, 2000},
                {"Clean-up", "Skin", 30, 0, 900},
                {"Manicure", "Nails", 45, 0, 700},
                {"Pedicure", "Nails", 45, 0, 800},
                {"Beard Trim", "Grooming", 20, 0, 300},
                {"Head Massage", "Wellness", 30, 0, 600},
                {"Bridal Makeup", "Makeup", 180, 0, 12000},
        };
        List<ServiceItem> services = new ArrayList<>();
        for (Object[] sd : serviceData) {
            ServiceItem s = new ServiceItem();
            s.setName((String) sd[0]);
            s.setCategory((String) sd[1]);
            s.setDurationMinutes((Integer) sd[2]);
            s.setProcessingMinutes((Integer) sd[3]);
            s.setPrice(BigDecimal.valueOf((Integer) sd[4]));
            services.add(serviceItemRepository.save(s));
        }

        // ~25 appointments spread over yesterday/today/next 5 days
        List<Client> clients = clientRepository.findAll();
        AppointmentStatus[] statuses = {
                AppointmentStatus.COMPLETED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW,
                AppointmentStatus.CONFIRMED, AppointmentStatus.CONFIRMED, AppointmentStatus.BOOKED,
                AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED,
                AppointmentStatus.BOOKED};
        LocalTime[] startTimes = {
                LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(12, 0),
                LocalTime.of(13, 0), LocalTime.of(14, 30), LocalTime.of(15, 0), LocalTime.of(16, 0),
                LocalTime.of(17, 0), LocalTime.of(18, 30)};
        int idx = 0;
        List<Appointment> seededAppointments = new ArrayList<>();
        for (int day = -1; day <= 5; day++) {
            LocalDate date = today.plusDays(day);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            int perDay = day <= 0 ? 4 : 3;
            for (int i = 0; i < perDay; i++) {
                Staff staff = staffList.get(idx % staffList.size());
                Client client = clients.get(idx % clients.size());
                ServiceItem svc = services.get(idx % services.size());
                // avoid overlapping with same staff on this day by staggering hour
                LocalDateTime start = date.atTime(startTimes[i % startTimes.length])
                        .plusMinutes(GRID_OFFSET * staffList.indexOf(staff));
                LocalDateTime end = start.plusMinutes(svc.getDurationMinutes() + svc.getProcessingMinutes());
                if (end.toLocalTime().isAfter(LocalTime.of(20, 0)) || end.isBefore(start)) {
                    idx++;
                    continue;
                }
                Appointment a = new Appointment();
                a.setClient(client);
                a.setStaff(staff);
                a.setStartTime(start);
                a.setEndTime(end);
                a.setServices(new ArrayList<>(List.of(svc)));
                a.setStatus(statuses[idx % statuses.length]);
                a.setSource(idx % 3 == 0 ? AppointmentSource.ONLINE : AppointmentSource.DESK);
                a.setTotalPrice(svc.getPrice());
                appointmentRepository.save(a);
                seededAppointments.add(a);
                idx++;
            }
        }

        seedSales(today, staffList, clients, services, seededAppointments);
    }

    private void seedSales(LocalDate today, List<Staff> staffList, List<Client> clients,
                           List<ServiceItem> services, List<Appointment> appointments) {
        // ~10 products, some low stock
        Object[][] productData = {
                {"Argan Shampoo 250ml", "SHP-ARG-250", "Haircare", 650, 24, 5},
                {"Keratin Serum 100ml", "SER-KER-100", "Haircare", 950, 18, 5},
                {"Hair Mask 200g", "MSK-REP-200", "Haircare", 1200, 3, 5},
                {"Vitamin C Serum 30ml", "SER-VTC-030", "Skincare", 1400, 12, 5},
                {"Sunscreen SPF50 50g", "SN-SPF50", "Skincare", 550, 30, 10},
                {"Nail Polish - Rouge", "NP-ROUGE", "Nails", 250, 2, 5},
                {"Beard Oil 30ml", "BD-OIL-30", "Grooming", 450, 15, 5},
                {"Dry Shampoo 150ml", "DRY-SHP-150", "Haircare", 700, 20, 5},
                {"Face Mist 100ml", "FM-ROSE-100", "Skincare", 380, 4, 5},
                {"Wax Strips Pack", "WX-STRP-20", "Consumables", 180, 40, 10},
        };
        List<Product> products = new ArrayList<>();
        for (Object[] pd : productData) {
            Product p = new Product();
            p.setName((String) pd[0]);
            p.setSku((String) pd[1]);
            p.setCategory((String) pd[2]);
            p.setPrice(BigDecimal.valueOf((Integer) pd[3]));
            p.setStockQty((Integer) pd[4]);
            p.setLowStockThreshold((Integer) pd[5]);
            products.add(productRepository.save(p));
        }

        // PAID sales for past COMPLETED appointments
        PaymentMethod[] methods = {PaymentMethod.UPI, PaymentMethod.CASH, PaymentMethod.CARD,
                PaymentMethod.UPI, PaymentMethod.WALLET};
        int inv = 0;
        for (Appointment a : appointments) {
            if (a.getStatus() != AppointmentStatus.COMPLETED) {
                continue;
            }
            Sale sale = paidSaleBase(a.getClient(), a.getStaff(), a,
                    a.getStartTime().toLocalDate().equals(today) ? today : a.getStartTime().toLocalDate());
            for (ServiceItem s : a.getServices()) {
                sale.getLines().add(saleLine(SaleLineType.SERVICE, s.getId(), s.getName(), 1, s.getPrice()));
            }
            if (inv % 2 == 0) {
                Product p = products.get(inv % products.size());
                sale.getLines().add(saleLine(SaleLineType.PRODUCT, p.getId(), p.getName(), 1, p.getPrice()));
            }
            finalizeSeedSale(sale, methods[inv % methods.length], ++inv);
        }

        // Walk-in and client sales over the last 30 days (no appointment)
        for (int i = 0; i < 8; i++) {
            Client client = i % 3 == 0 ? null : clients.get(i % clients.size());
            Staff staff = staffList.get(i % staffList.size());
            Sale sale = paidSaleBase(client, staff, null, today.minusDays(1 + i * 3));
            ServiceItem svc = services.get(i % services.size());
            sale.getLines().add(saleLine(SaleLineType.SERVICE, svc.getId(), svc.getName(), 1, svc.getPrice()));
            if (i % 2 == 1) {
                Product p = products.get(i % products.size());
                sale.getLines().add(saleLine(SaleLineType.PRODUCT, p.getId(), p.getName(), 2, p.getPrice()));
            }
            sale.setTipAmount(i % 4 == 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO);
            finalizeSeedSale(sale, methods[i % methods.length], ++inv);
        }
    }

    private Sale paidSaleBase(Client client, Staff staff, Appointment appointment, LocalDate date) {
        Sale sale = new Sale();
        sale.setClient(client);
        sale.setStaff(staff);
        sale.setAppointment(appointment);
        sale.setTaxRate(BigDecimal.valueOf(18));
        sale.setDiscountAmount(BigDecimal.ZERO);
        sale.setTipAmount(BigDecimal.ZERO);
        return sale;
    }

    private SaleLine saleLine(SaleLineType type, java.util.UUID refId, String name,
                              int qty, BigDecimal unitPrice) {
        SaleLine l = new SaleLine();
        l.setType(type);
        l.setRefId(refId);
        l.setName(name);
        l.setQuantity(qty);
        l.setUnitPrice(unitPrice);
        l.setDiscountAmount(BigDecimal.ZERO);
        return l;
    }

    private void finalizeSeedSale(Sale sale, PaymentMethod method, int invSeq) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleLine l : sale.getLines()) {
            BigDecimal lt = l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()))
                    .subtract(l.getDiscountAmount());
            l.setLineTotal(lt);
            subtotal = subtotal.add(lt);
        }
        BigDecimal taxable = subtotal.subtract(sale.getDiscountAmount());
        BigDecimal tax = taxable.multiply(sale.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        sale.setSubtotal(subtotal);
        sale.setTaxAmount(tax);
        sale.setTotal(taxable.add(tax).add(sale.getTipAmount()));
        Payment p = new Payment();
        p.setMethod(method);
        p.setAmount(sale.getTotal());
        p.setReference(method == PaymentMethod.UPI ? "upi@salon" + invSeq : null);
        sale.getPayments().add(p);
        sale.setStatus(SaleStatus.PAID);
        sale.setPaidAt(Instant.now().minusSeconds(86400L * (invSeq % 28)));
        sale.setInvoiceNumber("INV-" + LocalDate.now().getYear()
                + "-" + String.format("%04d", invSeq));
        saleRepository.save(sale);
    }

    private static final int GRID_OFFSET = 10;
}
