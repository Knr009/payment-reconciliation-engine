package com.nithin.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReport {

    private int totalTransactionsChecked;
    private int matchedCount;
    private int mismatchedCount;
    private int missingInSettlementCount;
    private int missingInLedgerCount;

    private List<Mismatch> mismatches;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mismatch {
        private String idempotencyKey;
        private String reason;
        private BigDecimal ledgerAmount;
        private BigDecimal settlementAmount;
    }
}
