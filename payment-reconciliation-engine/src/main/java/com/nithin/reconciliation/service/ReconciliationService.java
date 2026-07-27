package com.nithin.reconciliation.service;

import com.nithin.reconciliation.dto.ReconciliationReport;
import com.nithin.reconciliation.model.SettlementRecord;
import com.nithin.reconciliation.model.Transaction;
import com.nithin.reconciliation.model.TransactionStatus;
import com.nithin.reconciliation.repository.SettlementRecordRepository;
import com.nithin.reconciliation.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Matches internal ledger (Transaction) records against an external
 * settlement batch (SettlementRecord, simulating a bank/acquirer file).
 *
 * A transaction is considered:
 *  - MATCHED       - captured internally, present in settlement, amounts equal
 *  - MISMATCHED     - present on both sides but amounts differ (partial capture,
 *                      currency conversion issue, fee deduction, etc.)
 *  - MISSING_IN_SETTLEMENT - captured internally but never appeared in the
 *                      settlement file (possible processor-side failure)
 *  - MISSING_IN_LEDGER - present in settlement but no matching internal
 *                      record (possible duplicate charge or fraud signal)
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final SettlementRecordRepository settlementRecordRepository;

    @Transactional
    public ReconciliationReport reconcile() {
        List<Transaction> capturedTransactions = transactionRepository.findAll().stream()
            .filter(t -> t.getStatus() == TransactionStatus.CAPTURED || t.getStatus() == TransactionStatus.REFUNDED)
            .collect(java.util.stream.Collectors.toList());

        List<SettlementRecord> settlementRecords = settlementRecordRepository.findAll();

        int matched = 0;
        int mismatched = 0;
        int missingInSettlement = 0;
        List<ReconciliationReport.Mismatch> mismatches = new ArrayList<>();

        for (Transaction txn : capturedTransactions) {
            Optional<SettlementRecord> settlementMatch = settlementRecords.stream()
                .filter(s -> s.getIdempotencyKey().equals(txn.getIdempotencyKey()))
                .findFirst();

            if (settlementMatch.isEmpty()) {
                missingInSettlement++;
                mismatches.add(ReconciliationReport.Mismatch.builder()
                    .idempotencyKey(txn.getIdempotencyKey())
                    .reason("MISSING_IN_SETTLEMENT")
                    .ledgerAmount(txn.getAmount())
                    .settlementAmount(null)
                    .build());
                continue;
            }

            BigDecimal settledAmount = settlementMatch.get().getSettledAmount();
            if (settledAmount.compareTo(txn.getAmount()) != 0) {
                mismatched++;
                mismatches.add(ReconciliationReport.Mismatch.builder()
                    .idempotencyKey(txn.getIdempotencyKey())
                    .reason("AMOUNT_MISMATCH")
                    .ledgerAmount(txn.getAmount())
                    .settlementAmount(settledAmount)
                    .build());
            } else {
                matched++;
                txn.setReconciledAt(Instant.now());
                transactionRepository.save(txn);
            }
        }

        long missingInLedger = settlementRecords.stream()
            .filter(s -> capturedTransactions.stream()
                .noneMatch(t -> t.getIdempotencyKey().equals(s.getIdempotencyKey())))
            .count();

        for (SettlementRecord s : settlementRecords) {
            boolean hasLedgerMatch = capturedTransactions.stream()
                .anyMatch(t -> t.getIdempotencyKey().equals(s.getIdempotencyKey()));
            if (!hasLedgerMatch) {
                mismatches.add(ReconciliationReport.Mismatch.builder()
                    .idempotencyKey(s.getIdempotencyKey())
                    .reason("MISSING_IN_LEDGER")
                    .ledgerAmount(null)
                    .settlementAmount(s.getSettledAmount())
                    .build());
            }
        }

        return ReconciliationReport.builder()
            .totalTransactionsChecked(capturedTransactions.size())
            .matchedCount(matched)
            .mismatchedCount(mismatched)
            .missingInSettlementCount(missingInSettlement)
            .missingInLedgerCount((int) missingInLedger)
            .mismatches(mismatches)
            .build();
    }
}
