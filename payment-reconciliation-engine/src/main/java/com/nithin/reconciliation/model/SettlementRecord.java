package com.nithin.reconciliation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a single line item from an external bank / acquirer settlement
 * batch file (CSV in this simulation). Reconciliation matches these against
 * internal Transaction records by idempotencyKey.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String idempotencyKey;

    private BigDecimal settledAmount;

    private String currency;

    private Instant settlementDate;

    private String batchFileName;
}
