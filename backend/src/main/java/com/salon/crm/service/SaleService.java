package com.salon.crm.service;

import com.salon.crm.dto.PaymentRequest;
import com.salon.crm.dto.SaleLineRequest;
import com.salon.crm.dto.SaleLineResponse;
import com.salon.crm.dto.SaleRequest;
import com.salon.crm.dto.SaleResponse;
import com.salon.crm.dto.SaleSummaryResponse;
import com.salon.crm.entity.Appointment;
import com.salon.crm.entity.AppointmentStatus;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.Payment;
import com.salon.crm.entity.PaymentMethod;
import com.salon.crm.entity.Product;
import com.salon.crm.entity.Sale;
import com.salon.crm.entity.SaleLine;
import com.salon.crm.entity.SaleLineType;
import com.salon.crm.entity.SaleStatus;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.Visit;
import com.salon.crm.exception.BadRequestException;
import com.salon.crm.exception.NotFoundException;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ProductRepository;
import com.salon.crm.repository.SaleRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.VisitRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final ClientRepository clientRepository;
    private final StaffRepository staffRepository;
    private final AppointmentRepository appointmentRepository;
    private final VisitRepository visitRepository;
    private final SchedulingService schedulingService;
    private final BigDecimal taxRate;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       ServiceItemRepository serviceItemRepository,
                       ClientRepository clientRepository,
                       StaffRepository staffRepository,
                       AppointmentRepository appointmentRepository,
                       VisitRepository visitRepository,
                       SchedulingService schedulingService,
                       @Value("${crm.tax.rate:18}") BigDecimal taxRate) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.clientRepository = clientRepository;
        this.staffRepository = staffRepository;
        this.appointmentRepository = appointmentRepository;
        this.visitRepository = visitRepository;
        this.schedulingService = schedulingService;
        this.taxRate = taxRate;
    }

    // ---- Sales CRUD ----

    @Transactional(readOnly = true)
    public Page<SaleResponse> list(Instant from, Instant to, SaleStatus status,
                                   UUID clientId, Pageable pageable) {
        return saleRepository.search(from, to, status, clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SaleResponse get(UUID id) {
        return toResponse(findSale(id));
    }

    public SaleResponse createDraft(SaleRequest req) {
        Sale sale = new Sale();
        applySale(sale, req);
        return toResponse(saleRepository.save(sale));
    }

    public SaleResponse updateDraft(UUID id, SaleRequest req) {
        Sale sale = findSale(id);
        requireDraft(sale);
        applySale(sale, req);
        return toResponse(saleRepository.save(sale));
    }

    public SaleResponse addPayment(UUID id, PaymentRequest req) {
        Sale sale = findSale(id);
        requireDraft(sale);
        Payment p = new Payment();
        p.setMethod(req.method());
        p.setAmount(req.amount());
        p.setReference(req.reference());
        sale.getPayments().add(p);
        return toResponse(saleRepository.save(sale));
    }

    public SaleResponse removePayment(UUID id, UUID paymentId) {
        Sale sale = findSale(id);
        requireDraft(sale);
        boolean removed = sale.getPayments().removeIf(p -> p.getId().equals(paymentId));
        if (!removed) {
            throw new NotFoundException("Payment not found: " + paymentId);
        }
        return toResponse(saleRepository.save(sale));
    }

    // ---- Lifecycle ----

    public SaleResponse pay(UUID id) {
        Sale sale = findSale(id);
        requireDraft(sale);
        if (sale.getLines().isEmpty()) {
            throw new BadRequestException("Cannot pay a sale with no lines");
        }
        BigDecimal paid = sale.getPayments().stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(sale.getTotal()) < 0) {
            throw new BadRequestException("Insufficient payment: shortfall ₹"
                    + sale.getTotal().subtract(paid));
        }

        List<String> warnings = new ArrayList<>();
        for (SaleLine line : sale.getLines()) {
            if (line.getType() == SaleLineType.PRODUCT && line.getRefId() != null) {
                Product product = productRepository.findById(line.getRefId()).orElse(null);
                if (product != null) {
                    product.setStockQty(product.getStockQty() - line.getQuantity());
                    productRepository.save(product);
                    if (product.getStockQty() <= product.getLowStockThreshold()) {
                        warnings.add("Low stock: " + product.getName()
                                + " (" + product.getStockQty() + " left)");
                    }
                }
            }
        }

        sale.setStatus(SaleStatus.PAID);
        sale.setPaidAt(Instant.now());
        sale.setInvoiceNumber(nextInvoiceNumber());
        Sale saved = saleRepository.save(sale);

        // CRM link: complete appointment, sync its visit
        Appointment appt = sale.getAppointment();
        if (appt != null) {
            if (appt.getStatus() != AppointmentStatus.COMPLETED) {
                schedulingService.updateStatus(appt.getId(),
                        new com.salon.crm.dto.AppointmentStatusRequest(AppointmentStatus.COMPLETED));
                appointmentRepository.flush();
                appt = appointmentRepository.findById(appt.getId()).orElseThrow();
            }
            Visit visit = appt.getVisit();
            if (visit == null) {
                visit = new Visit();
                visit.setClient(appt.getClient());
                visit.setVisitDate(appt.getStartTime().toLocalDate());
                visit.setStylistName(appt.getStaff() != null ? appt.getStaff().getName() : null);
                appt.setVisit(visit);
            }
            visit.setAmount(saved.getTotal());
            visit.setServices(new ArrayList<>(saved.getLines().stream()
                    .filter(l -> l.getType() == SaleLineType.SERVICE)
                    .map(SaleLine::getName).toList()));
            visit.setProducts(new ArrayList<>(saved.getLines().stream()
                    .filter(l -> l.getType() == SaleLineType.PRODUCT)
                    .map(SaleLine::getName).toList()));
            visitRepository.save(visit);
        } else if (sale.getClient() != null) {
            Visit visit = new Visit();
            visit.setClient(sale.getClient());
            visit.setVisitDate(LocalDate.now());
            visit.setStylistName(sale.getStaff() != null ? sale.getStaff().getName() : null);
            visit.setAmount(saved.getTotal());
            visit.setServices(new ArrayList<>(saved.getLines().stream()
                    .filter(l -> l.getType() == SaleLineType.SERVICE)
                    .map(SaleLine::getName).toList()));
            visit.setProducts(new ArrayList<>(saved.getLines().stream()
                    .filter(l -> l.getType() == SaleLineType.PRODUCT)
                    .map(SaleLine::getName).toList()));
            visitRepository.save(visit);
        }

        return toResponse(saved, warnings);
    }

    public SaleResponse voidSale(UUID id) {
        Sale sale = findSale(id);
        if (sale.getStatus() != SaleStatus.PAID && sale.getStatus() != SaleStatus.DRAFT) {
            throw new BadRequestException("Cannot void a sale in status " + sale.getStatus());
        }
        if (sale.getStatus() == SaleStatus.PAID) {
            restoreStock(sale);
        }
        sale.setStatus(SaleStatus.VOID);
        return toResponse(saleRepository.save(sale));
    }

    public SaleResponse refund(UUID id) {
        Sale sale = findSale(id);
        if (sale.getStatus() != SaleStatus.PAID) {
            throw new BadRequestException("Only PAID sales can be refunded");
        }
        restoreStock(sale);
        sale.setStatus(SaleStatus.REFUNDED);
        return toResponse(saleRepository.save(sale));
    }

    // ---- from-appointment ----

    public SaleResponse createFromAppointment(UUID appointmentId) {
        var existing = saleRepository.findByAppointmentIdAndStatus(appointmentId, SaleStatus.DRAFT);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));
        Sale sale = new Sale();
        sale.setClient(appt.getClient());
        sale.setStaff(appt.getStaff());
        sale.setAppointment(appt);
        sale.setTaxRate(taxRate);
        for (var svc : appt.getServices()) {
            SaleLine line = new SaleLine();
            line.setType(SaleLineType.SERVICE);
            line.setRefId(svc.getId());
            line.setName(svc.getName());
            line.setQuantity(1);
            line.setUnitPrice(svc.getPrice());
            sale.getLines().add(line);
        }
        recompute(sale);
        return toResponse(saleRepository.save(sale));
    }

    // ---- summary ----

    @Transactional(readOnly = true)
    public SaleSummaryResponse summary(Instant from, Instant to) {
        List<Sale> sales = saleRepository.search(from, to, SaleStatus.PAID, null,
                Pageable.unpaged()).getContent();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal serviceRev = BigDecimal.ZERO;
        BigDecimal retailRev = BigDecimal.ZERO;
        BigDecimal tipTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        for (Sale s : sales) {
            revenue = revenue.add(s.getTotal());
            tipTotal = tipTotal.add(s.getTipAmount() != null ? s.getTipAmount() : BigDecimal.ZERO);
            for (SaleLine l : s.getLines()) {
                BigDecimal lt = l.getLineTotal() != null ? l.getLineTotal() : BigDecimal.ZERO;
                if (l.getType() == SaleLineType.SERVICE) serviceRev = serviceRev.add(lt);
                else retailRev = retailRev.add(lt);
            }
            for (Payment p : s.getPayments()) {
                byMethod.merge(p.getMethod().name(), p.getAmount(), BigDecimal::add);
            }
        }
        long count = sales.size();
        BigDecimal avg = count > 0
                ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return new SaleSummaryResponse(revenue, count, avg, serviceRev, retailRev, tipTotal, byMethod);
    }

    // ---- internals ----

    private void applySale(Sale sale, SaleRequest req) {
        sale.setClient(req.clientId() != null
                ? clientRepository.findById(req.clientId())
                    .orElseThrow(() -> new NotFoundException("Client not found: " + req.clientId()))
                : null);
        sale.setStaff(req.staffId() != null
                ? staffRepository.findById(req.staffId())
                    .orElseThrow(() -> new NotFoundException("Staff not found: " + req.staffId()))
                : null);
        if (req.appointmentId() != null) {
            sale.setAppointment(appointmentRepository.findById(req.appointmentId())
                    .orElseThrow(() -> new NotFoundException("Appointment not found: " + req.appointmentId())));
        }
        sale.getLines().clear();
        if (req.lines() != null) {
            for (SaleLineRequest lr : req.lines()) {
                sale.getLines().add(buildLine(lr));
            }
        }
        sale.setDiscountAmount(req.discountAmount() != null ? req.discountAmount() : BigDecimal.ZERO);
        sale.setTipAmount(req.tipAmount() != null ? req.tipAmount() : BigDecimal.ZERO);
        sale.setNotes(req.notes());
        sale.setTaxRate(taxRate);
        recompute(sale);
    }

    private SaleLine buildLine(SaleLineRequest lr) {
        SaleLine line = new SaleLine();
        line.setType(lr.type());
        line.setRefId(lr.refId());
        line.setQuantity(lr.quantity());
        if (lr.type() == SaleLineType.SERVICE) {
            var svc = serviceItemRepository.findById(lr.refId())
                    .orElseThrow(() -> new NotFoundException("Service not found: " + lr.refId()));
            line.setName(svc.getName());
            line.setUnitPrice(svc.getPrice());
        } else {
            Product p = productRepository.findById(lr.refId())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + lr.refId()));
            line.setName(p.getName());
            line.setUnitPrice(p.getPrice());
        }
        line.setDiscountAmount(lr.discountAmount() != null ? lr.discountAmount() : BigDecimal.ZERO);
        return line;
    }

    private void recompute(Sale sale) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleLine line : sale.getLines()) {
            BigDecimal gross = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            BigDecimal lt = gross.subtract(line.getDiscountAmount() != null
                    ? line.getDiscountAmount() : BigDecimal.ZERO);
            line.setLineTotal(lt);
            subtotal = subtotal.add(lt);
        }
        BigDecimal discount = sale.getDiscountAmount() != null ? sale.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal tip = sale.getTipAmount() != null ? sale.getTipAmount() : BigDecimal.ZERO;
        BigDecimal taxable = subtotal.subtract(discount);
        if (taxable.signum() < 0) taxable = BigDecimal.ZERO;
        BigDecimal tax = taxable.multiply(sale.getTaxRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        sale.setSubtotal(subtotal);
        sale.setTaxAmount(tax);
        sale.setTotal(taxable.add(tax).add(tip));
    }

    private void restoreStock(Sale sale) {
        for (SaleLine line : sale.getLines()) {
            if (line.getType() == SaleLineType.PRODUCT && line.getRefId() != null) {
                productRepository.findById(line.getRefId()).ifPresent(p -> {
                    p.setStockQty(p.getStockQty() + line.getQuantity());
                    productRepository.save(p);
                });
            }
        }
    }

    private String nextInvoiceNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String max = saleRepository.findMaxInvoiceNumber(year);
        int next = 1;
        if (max != null) {
            next = Integer.parseInt(max.substring(max.lastIndexOf('-') + 1)) + 1;
        }
        return "INV-" + year + "-" + String.format("%04d", next);
    }

    private void requireDraft(Sale sale) {
        if (sale.getStatus() != SaleStatus.DRAFT) {
            throw new BadRequestException("Sale can only be edited while DRAFT (current: "
                    + sale.getStatus() + ")");
        }
    }

    private Sale findSale(UUID id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sale not found: " + id));
    }

    private SaleResponse toResponse(Sale s) {
        return toResponse(s, List.of());
    }

    private SaleResponse toResponse(Sale s, List<String> warnings) {
        Client c = s.getClient();
        Staff st = s.getStaff();
        return new SaleResponse(
                s.getId(), s.getInvoiceNumber(),
                c != null ? c.getId() : null,
                c != null ? (c.getFirstName() + " "
                        + (c.getLastName() != null ? c.getLastName() : "")).trim() : "Walk-in",
                c != null ? c.getPhone() : null,
                s.getAppointment() != null ? s.getAppointment().getId() : null,
                st != null ? st.getId() : null,
                st != null ? st.getName() : null,
                s.getLines().stream().map(this::toLineResponse).toList(),
                s.getDiscountAmount(), s.getTaxRate(), s.getSubtotal(),
                s.getTaxAmount(), s.getTipAmount(), s.getTotal(),
                s.getPayments().stream().map(p -> new SaleResponse.PaymentDto(
                        p.getId(), p.getMethod(), p.getAmount(), p.getReference(), p.getPaidAt())).toList(),
                s.getStatus(), s.getNotes(), s.getPaidAt(), s.getCreatedAt(), warnings);
    }

    private SaleLineResponse toLineResponse(SaleLine l) {
        return new SaleLineResponse(l.getId(), l.getType(), l.getRefId(), l.getName(),
                l.getQuantity(), l.getUnitPrice(), l.getDiscountAmount(), l.getLineTotal());
    }
}
