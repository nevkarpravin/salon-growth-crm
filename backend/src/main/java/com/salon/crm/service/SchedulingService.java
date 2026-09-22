package com.salon.crm.service;

import com.salon.crm.dto.AppointmentRequest;
import com.salon.crm.dto.AppointmentResponse;
import com.salon.crm.dto.AppointmentStatusRequest;
import com.salon.crm.dto.AvailabilityResponse;
import com.salon.crm.dto.ServiceItemRequest;
import com.salon.crm.dto.ServiceItemResponse;
import com.salon.crm.dto.StaffRequest;
import com.salon.crm.dto.StaffResponse;
import com.salon.crm.dto.WorkingHoursRequest;
import com.salon.crm.entity.Appointment;
import com.salon.crm.entity.AppointmentStatus;
import com.salon.crm.entity.AppointmentSource;
import com.salon.crm.entity.Client;
import com.salon.crm.entity.ServiceItem;
import com.salon.crm.entity.Staff;
import com.salon.crm.entity.Visit;
import com.salon.crm.entity.WorkingHours;
import com.salon.crm.exception.AppointmentConflictException;
import com.salon.crm.exception.BadRequestException;
import com.salon.crm.exception.NotFoundException;
import com.salon.crm.repository.AppointmentRepository;
import com.salon.crm.repository.ClientRepository;
import com.salon.crm.repository.ServiceItemRepository;
import com.salon.crm.repository.StaffRepository;
import com.salon.crm.repository.VisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SchedulingService {

    private static final int GRID_MINUTES = 15;
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            AppointmentStatus.BOOKED, EnumSet.of(AppointmentStatus.CONFIRMED,
                    AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.COMPLETED),
            AppointmentStatus.CONFIRMED, EnumSet.of(AppointmentStatus.COMPLETED,
                    AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW));

    private final StaffRepository staffRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final VisitRepository visitRepository;

    public SchedulingService(StaffRepository staffRepository,
                             ServiceItemRepository serviceItemRepository,
                             AppointmentRepository appointmentRepository,
                             ClientRepository clientRepository,
                             VisitRepository visitRepository) {
        this.staffRepository = staffRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
        this.visitRepository = visitRepository;
    }

    // ---- Staff ----

    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff(Boolean activeOnly) {
        List<Staff> staff = Boolean.TRUE.equals(activeOnly)
                ? staffRepository.findByActiveTrue()
                : staffRepository.findAll();
        return staff.stream().map(this::toStaffResponse).toList();
    }

    public StaffResponse createStaff(StaffRequest req) {
        Staff s = new Staff();
        applyStaff(s, req);
        return toStaffResponse(staffRepository.save(s));
    }

    public StaffResponse updateStaff(UUID id, StaffRequest req) {
        Staff s = findStaff(id);
        applyStaff(s, req);
        return toStaffResponse(staffRepository.save(s));
    }

    public void deleteStaff(UUID id) {
        Staff s = findStaff(id);
        s.setActive(false);
        staffRepository.save(s);
    }

    public StaffResponse setWorkingHours(UUID id, WorkingHoursRequest req) {
        Staff s = findStaff(id);
        List<WorkingHours> hours = new ArrayList<>();
        for (WorkingHoursRequest.Entry e : req.hours()) {
            WorkingHours wh = new WorkingHours();
            wh.setDayOfWeek(DayOfWeek.valueOf(e.dayOfWeek()));
            wh.setStartTime(LocalTime.parse(e.startTime()));
            wh.setEndTime(LocalTime.parse(e.endTime()));
            hours.add(wh);
        }
        s.setWorkingHours(hours);
        return toStaffResponse(staffRepository.save(s));
    }

    // ---- Services ----

    @Transactional(readOnly = true)
    public List<ServiceItemResponse> listServices(Boolean activeOnly) {
        List<ServiceItem> items = Boolean.TRUE.equals(activeOnly)
                ? serviceItemRepository.findByActiveTrue()
                : serviceItemRepository.findAll();
        return items.stream().map(this::toServiceResponse).toList();
    }

    public ServiceItemResponse createService(ServiceItemRequest req) {
        ServiceItem s = new ServiceItem();
        applyService(s, req);
        return toServiceResponse(serviceItemRepository.save(s));
    }

    public ServiceItemResponse updateService(UUID id, ServiceItemRequest req) {
        ServiceItem s = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
        applyService(s, req);
        return toServiceResponse(serviceItemRepository.save(s));
    }

    public void deleteService(UUID id) {
        ServiceItem s = serviceItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
        s.setActive(false);
        serviceItemRepository.save(s);
    }

    // ---- Appointments ----

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointments(LocalDateTime from, LocalDateTime to,
                                                      UUID staffId, AppointmentStatus status,
                                                      UUID clientId) {
        return appointmentRepository.findInRange(from, to, staffId, status, clientId).stream()
                .map(this::toAppointmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(UUID id) {
        return toAppointmentResponse(findAppointment(id));
    }

    public AppointmentResponse createAppointment(AppointmentRequest req) {
        Appointment a = new Appointment();
        applyAppointment(a, req);
        a.setStatus(AppointmentStatus.BOOKED);
        return toAppointmentResponse(appointmentRepository.save(a));
    }

    public AppointmentResponse updateAppointment(UUID id, AppointmentRequest req) {
        Appointment a = findAppointment(id);
        if (a.getStatus() == AppointmentStatus.COMPLETED || a.getStatus() == AppointmentStatus.CANCELLED
                || a.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new BadRequestException("Cannot edit an appointment in status " + a.getStatus());
        }
        applyAppointment(a, req);
        return toAppointmentResponse(appointmentRepository.save(a));
    }

    public AppointmentResponse updateStatus(UUID id, AppointmentStatusRequest req) {
        Appointment a = findAppointment(id);
        AppointmentStatus target = req.status();
        Set<AppointmentStatus> allowed = TRANSITIONS.getOrDefault(a.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new BadRequestException(
                    "Cannot transition appointment from " + a.getStatus() + " to " + target);
        }
        a.setStatus(target);
        Appointment saved = appointmentRepository.save(a);
        if (target == AppointmentStatus.COMPLETED) {
            Visit visit = new Visit();
            visit.setClient(a.getClient());
            visit.setVisitDate(a.getStartTime().toLocalDate());
            visit.setStylistName(a.getStaff().getName());
            visit.setServices(a.getServices().stream().map(ServiceItem::getName).toList());
            visit.setAmount(a.getTotalPrice());
            visit.setNotes(a.getNotes());
            visitRepository.save(visit);
        }
        return toAppointmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID staffId, LocalDate date, List<UUID> serviceIds) {
        Staff staff = findStaff(staffId);
        List<ServiceItem> services = resolveServices(serviceIds);
        int duration = totalMinutes(services);
        if (duration <= 0) {
            throw new BadRequestException("Total service duration must be positive");
        }
        WorkingHours hours = workingHoursFor(staff, date.getDayOfWeek());
        if (hours == null) {
            return new AvailabilityResponse(List.of());
        }
        LocalDateTime dayStart = date.atTime(hours.getStartTime());
        LocalDateTime dayEnd = date.atTime(hours.getEndTime());
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> booked = appointmentRepository.findOverlapping(
                staffId, dayStart, dayEnd, null);

        List<AvailabilityResponse.Slot> slots = new ArrayList<>();
        for (LocalDateTime start = dayStart; !start.plusMinutes(duration).isAfter(dayEnd);
             start = start.plusMinutes(GRID_MINUTES)) {
            LocalDateTime slotStart = start;
            LocalDateTime slotEnd = start.plusMinutes(duration);
            if (slotEnd.isBefore(now)) {
                continue;
            }
            boolean overlaps = booked.stream().anyMatch(a ->
                    a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart));
            if (!overlaps) {
                slots.add(new AvailabilityResponse.Slot(slotStart, slotEnd));
            }
        }
        return new AvailabilityResponse(slots);
    }

    // ---- internals ----

    private void applyStaff(Staff s, StaffRequest req) {
        s.setName(req.name());
        if (req.role() != null) s.setRole(req.role());
        s.setPhone(req.phone());
        s.setColorHex(req.colorHex());
        if (req.active() != null) s.setActive(req.active());
    }

    private void applyService(ServiceItem s, ServiceItemRequest req) {
        s.setName(req.name());
        s.setCategory(req.category());
        s.setDurationMinutes(req.durationMinutes());
        s.setProcessingMinutes(req.processingMinutes() != null ? req.processingMinutes() : 0);
        s.setPrice(req.price());
        if (req.active() != null) s.setActive(req.active());
    }

    private void applyAppointment(Appointment a, AppointmentRequest req) {
        Client client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + req.clientId()));
        Staff staff = findStaff(req.staffId());
        List<ServiceItem> services = resolveServices(req.serviceIds());
        int duration = totalMinutes(services);
        LocalDateTime end = req.startTime().plusMinutes(duration);

        if (!staff.getWorkingHours().isEmpty()) {
            WorkingHours hours = workingHoursFor(staff, req.startTime().getDayOfWeek());
            if (hours == null) {
                throw new BadRequestException("Staff member " + staff.getName()
                        + " does not work on " + req.startTime().getDayOfWeek());
            }
            LocalDateTime dayStart = req.startTime().toLocalDate().atTime(hours.getStartTime());
            LocalDateTime dayEnd = req.startTime().toLocalDate().atTime(hours.getEndTime());
            if (req.startTime().isBefore(dayStart) || end.isAfter(dayEnd)) {
                throw new BadRequestException("Appointment is outside working hours for "
                        + staff.getName() + " on " + req.startTime().getDayOfWeek()
                        + " (" + hours.getStartTime() + "–" + hours.getEndTime() + ")");
            }
        }

        List<Appointment> overlapping = appointmentRepository.findOverlapping(
                staff.getId(), req.startTime(), end, a.getId());
        if (!overlapping.isEmpty()) {
            throw new AppointmentConflictException("Staff member " + staff.getName()
                    + " already has an appointment overlapping "
                    + req.startTime() + "–" + end);
        }

        a.setClient(client);
        a.setStaff(staff);
        a.setStartTime(req.startTime());
        a.setEndTime(end);
        a.setServices(services);
        a.setSource(req.source() != null ? req.source() : AppointmentSource.DESK);
        a.setNotes(req.notes());
        a.setTotalPrice(services.stream()
                .map(s -> s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private List<ServiceItem> resolveServices(List<UUID> serviceIds) {
        List<ServiceItem> services = serviceItemRepository.findByIdIn(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new NotFoundException("One or more services not found");
        }
        return services;
    }

    private int totalMinutes(List<ServiceItem> services) {
        return services.stream()
                .mapToInt(s -> s.getDurationMinutes() + s.getProcessingMinutes()).sum();
    }

    private WorkingHours workingHoursFor(Staff staff, DayOfWeek day) {
        if (staff.getWorkingHours() == null || staff.getWorkingHours().isEmpty()) {
            return null; // skip check if none defined? caller decides; null also means "closed" for availability
        }
        return staff.getWorkingHours().stream()
                .filter(wh -> wh.getDayOfWeek() == day).findFirst().orElse(null);
    }

    private Staff findStaff(UUID id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Staff not found: " + id));
    }

    private Appointment findAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + id));
    }

    private StaffResponse toStaffResponse(Staff s) {
        return new StaffResponse(s.getId(), s.getName(), s.getRole(), s.getPhone(),
                s.getColorHex(), s.isActive(),
                s.getWorkingHours().stream()
                        .map(wh -> new StaffResponse.WorkingHoursDto(
                                wh.getDayOfWeek().name(),
                                wh.getStartTime().toString(),
                                wh.getEndTime().toString()))
                        .toList());
    }

    private ServiceItemResponse toServiceResponse(ServiceItem s) {
        return new ServiceItemResponse(s.getId(), s.getName(), s.getCategory(),
                s.getDurationMinutes(), s.getProcessingMinutes(), s.getPrice(), s.isActive());
    }

    private AppointmentResponse toAppointmentResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getClient().getId(),
                (a.getClient().getFirstName() + " "
                        + (a.getClient().getLastName() != null ? a.getClient().getLastName() : "")).trim(),
                a.getClient().getPhone(),
                a.getStaff().getId(),
                a.getStaff().getName(),
                a.getStaff().getColorHex(),
                a.getStartTime(),
                a.getEndTime(),
                a.getServices().stream()
                        .map(s -> new AppointmentResponse.ServiceRef(
                                s.getId(), s.getName(), s.getDurationMinutes(), s.getPrice()))
                        .toList(),
                a.getStatus(),
                a.getSource(),
                a.getNotes(),
                a.getTotalPrice());
    }
}
