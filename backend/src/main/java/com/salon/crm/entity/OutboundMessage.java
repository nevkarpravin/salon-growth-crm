package com.salon.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbound_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class OutboundMessage {

    @Id
    @GeneratedValue
    private UUID id;

    private String toPhone;

    @Column(columnDefinition = "text")
    private String body;

    private String channel = "WHATSAPP";

    private String status;

    private String providerMessageId;

    @Column(columnDefinition = "text")
    private String error;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private QueueTicket ticket;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
