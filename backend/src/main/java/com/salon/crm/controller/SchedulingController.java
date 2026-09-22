package com.salon.crm.controller;

import com.salon.crm.dto.AppointmentRequest;
import com.salon.crm.dto.AppointmentResponse;
import com.salon.crm.dto.AppointmentStatusRequest;
import com.salon.crm.dto.AvailabilityResponse;
import com.salon.crm.dto.ServiceItemRequest;
import com.salon.crm.dto.ServiceItemResponse;
import com.salon.crm.dto.StaffRequest;
import com.salon.crm.dto.StaffResponse;
import com.salon.crm.dto.WorkingHoursRequest;
import com.salon.crm.entity.AppointmentStatus;
import com.salon.crm.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class SchedulingController {

    private final SchedulingService schedulingService;

    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    // Staff

    @GetMapping("/staff")
    public List<StaffResponse> listStaff(@RequestParam(required = false) Boolean active) {
        return schedulingService.listStaff(active);
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffResponse> createStaff(@Valid @RequestBody StaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.createStaff(req));
    }

    @GetMapping("/staff/{id}")
    public StaffResponse getStaff(@PathVariable UUID id) {
        return schedulingService.listStaff(null).stream()
                .filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new com.salon.crm.exception.NotFoundException("Staff not found: " + id));
    }

    @PutMapping("/staff/{id}")
    public StaffResponse updateStaff(@PathVariable UUID id, @Valid @RequestBody StaffRequest req) {
        return schedulingService.updateStaff(id, req);
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID id) {
        schedulingService.deleteStaff(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/staff/{id}/working-hours")
    public StaffResponse setWorkingHours(@PathVariable UUID id,
                                         @Valid @RequestBody WorkingHoursRequest req) {
        return schedulingService.setWorkingHours(id, req);
    }

    // Services

    @GetMapping("/services")
    public List<ServiceItemResponse> listServices(@RequestParam(required = false) Boolean active) {
        return schedulingService.listServices(active);
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceItemResponse> createService(@Valid @RequestBody ServiceItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.createService(req));
    }

    @GetMapping("/services/{id}")
    public ServiceItemResponse getService(@PathVariable UUID id) {
        return schedulingService.listServices(null).stream()
                .filter(s -> s.id().equals(id)).findFirst()
                .orElseThrow(() -> new com.salon.crm.exception.NotFoundException("Service not found: " + id));
    }

    @PutMapping("/services/{id}")
    public ServiceItemResponse updateService(@PathVariable UUID id,
                                             @Valid @RequestBody ServiceItemRequest req) {
        return schedulingService.updateService(id, req);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        schedulingService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    // Appointments

    @GetMapping("/appointments")
    public List<AppointmentResponse> listAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) UUID staffId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) UUID clientId) {
        return schedulingService.listAppointments(from, to, staffId, status, clientId);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schedulingService.createAppointment(req));
    }

    @GetMapping("/appointments/{id}")
    public AppointmentResponse getAppointment(@PathVariable UUID id) {
        return schedulingService.getAppointment(id);
    }

    @PutMapping("/appointments/{id}")
    public AppointmentResponse updateAppointment(@PathVariable UUID id,
                                                 @Valid @RequestBody AppointmentRequest req) {
        return schedulingService.updateAppointment(id, req);
    }

    @PatchMapping("/appointments/{id}/status")
    public AppointmentResponse updateStatus(@PathVariable UUID id,
                                            @Valid @RequestBody AppointmentStatusRequest req) {
        return schedulingService.updateStatus(id, req);
    }

    @GetMapping("/appointments/availability")
    public AvailabilityResponse availability(
            @RequestParam UUID staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam List<UUID> serviceIds) {
        return schedulingService.availability(staffId, date, serviceIds);
    }
}
