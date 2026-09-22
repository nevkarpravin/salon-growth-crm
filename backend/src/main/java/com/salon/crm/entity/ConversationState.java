package com.salon.crm.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversation_states")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class ConversationState {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private ConversationStep step = ConversationStep.MENU;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_draft_services", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "service_id")
    private List<UUID> draftServiceIds = new ArrayList<>();

    private UUID draftStaffId;

    private UUID activeTicketId;

    @LastModifiedDate
    private Instant updatedAt;
}
