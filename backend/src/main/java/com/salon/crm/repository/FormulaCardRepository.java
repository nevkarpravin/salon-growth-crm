package com.salon.crm.repository;

import com.salon.crm.entity.FormulaCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormulaCardRepository extends JpaRepository<FormulaCard, UUID> {

    List<FormulaCard> findByClientIdOrderByRecordedAtDesc(UUID clientId);

    Optional<FormulaCard> findByIdAndClientId(UUID id, UUID clientId);
}
