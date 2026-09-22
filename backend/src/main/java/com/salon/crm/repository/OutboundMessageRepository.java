package com.salon.crm.repository;

import com.salon.crm.entity.OutboundMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboundMessageRepository extends JpaRepository<OutboundMessage, UUID> {

    List<OutboundMessage> findTop100ByOrderByCreatedAtDesc();

    List<OutboundMessage> findByToPhoneOrderByCreatedAtAsc(String toPhone);
}
