package com.salon.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sale_lines")
@Getter
@Setter
public class SaleLine {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleLineType type;

    private UUID refId;

    @Column(nullable = false)
    private String name;

    private int quantity = 1;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    private BigDecimal lineTotal;
}
