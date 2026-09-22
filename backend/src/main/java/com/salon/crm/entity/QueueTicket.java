package com.salon.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "queue_tickets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class QueueTicket {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "queue_ticket_services",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private List<ServiceItem> services = new ArrayList<>();

    private int tokenNumber;

    private LocalDate queueDate;

    private double queueOrder;

    @Enumerated(EnumType.STRING)
    private QueueTicketStatus status = QueueTicketStatus.WAITING;

    private int skipCount;

    private String source;

    private Instant joinedAt;
    private Instant nextNotifiedAt;
    private Instant calledAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant closedAt;

    private String cancelReason;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.NONE;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String paymentReference;

    @Column(columnDefinition = "text")
    private String paymentLink;

    private Instant paidAt;

    private Instant reviewRequestedAt;

    private Integer rating;

    @Column(columnDefinition = "text")
    private String reviewComment;

    private Instant reviewedAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
