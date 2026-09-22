package com.salon.crm.repository;

import com.salon.crm.entity.Appointment;
import com.salon.crm.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT a FROM Appointment a WHERE a.startTime < :end AND a.endTime > :start " +
            "AND a.staff.id = :staffId AND a.status <> 'CANCELLED' AND (:excludeId IS NULL OR a.id <> :excludeId)")
    List<Appointment> findOverlapping(@Param("staffId") UUID staffId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("excludeId") UUID excludeId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.staff JOIN FETCH a.client " +
            "WHERE a.startTime < :to AND a.endTime >= :from " +
            "AND (:staffId IS NULL OR a.staff.id = :staffId) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:clientId IS NULL OR a.client.id = :clientId) " +
            "ORDER BY a.startTime")
    List<Appointment> findInRange(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("staffId") UUID staffId,
                                  @Param("status") AppointmentStatus status,
                                  @Param("clientId") UUID clientId);
}
