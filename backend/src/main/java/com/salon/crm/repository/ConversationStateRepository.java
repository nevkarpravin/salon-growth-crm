package com.salon.crm.repository;

import com.salon.crm.entity.ConversationState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationStateRepository extends JpaRepository<ConversationState, UUID> {

    Optional<ConversationState> findByPhone(String phone);
}
