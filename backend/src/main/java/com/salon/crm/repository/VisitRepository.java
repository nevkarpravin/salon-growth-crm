package com.salon.crm.repository;

import com.salon.crm.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    List<Visit> findByClientIdOrderByVisitDateDesc(UUID clientId);

    Optional<Visit> findByIdAndClientId(UUID id, UUID clientId);

    @Query("SELECT MAX(v.visitDate) FROM Visit v WHERE v.client.id = :clientId")
    java.time.LocalDate findLastVisitDate(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(v) FROM Visit v WHERE v.client.id = :clientId")
    long countByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM Visit v WHERE v.client.id = :clientId")
    BigDecimal sumAmountByClientId(@Param("clientId") UUID clientId);
}
