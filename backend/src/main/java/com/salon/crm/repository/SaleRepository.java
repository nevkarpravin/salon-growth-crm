package com.salon.crm.repository;

import com.salon.crm.entity.Sale;
import com.salon.crm.entity.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    Optional<Sale> findByAppointmentIdAndStatus(UUID appointmentId, SaleStatus status);

    List<Sale> findByAppointmentId(UUID appointmentId);

    @Query("SELECT MAX(s.invoiceNumber) FROM Sale s WHERE s.invoiceNumber LIKE CONCAT('INV-', :year, '-%')")
    String findMaxInvoiceNumber(@Param("year") String year);

    @Query("SELECT s FROM Sale s WHERE (:from IS NULL OR s.createdAt >= :from) " +
            "AND (:to IS NULL OR s.createdAt < :to) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:clientId IS NULL OR s.client.id = :clientId) " +
            "ORDER BY s.createdAt DESC")
    Page<Sale> search(@Param("from") Instant from,
                      @Param("to") Instant to,
                      @Param("status") SaleStatus status,
                      @Param("clientId") UUID clientId,
                      Pageable pageable);
}
