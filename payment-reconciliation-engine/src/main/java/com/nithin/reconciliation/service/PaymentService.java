package com.nithin.reconciliation.service;

import com.nithin.reconciliation.dto.TransactionRequest;
import com.nithin.reconciliation.model.Transaction;
import com.nithin.reconciliation.model.TransactionStatus;
import com.nithin.reconciliation.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core payment lifecycle service.
 *
 * Idempotency strategy: every mutating call is keyed by a client-supplied
 * idempotencyKey with a unique DB constraint. On a retry with the same key,
 * we return the existing record instead of reprocessing - this is what
 * prevents double-authorization or double-capture when a client retries
 * after a timeout.
 *
 * State transitions are enforced through TransactionStatus.canTransitionTo,
 * so an out-of-order event (e.g. refund before capture) fails loudly instead
 * of silently corrupting the ledger.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction authorize(TransactionRequest request) {
        return transactionRepository.findByIdempotencyKey(request.getIdempotencyKey())
            .orElseGet(() -> {
                Instant now = Instant.now();
                Transaction txn = Transaction.builder()
                    .idempotencyKey(request.getIdempotencyKey())
                    .merchantId(request.getMerchantId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status(TransactionStatus.AUTHORIZED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
                return transactionRepository.save(txn);
            });
    }

    @Transactional
    public Transaction capture(String idempotencyKey) {
        Transaction txn = getOrThrow(idempotencyKey);
        // Already captured (retry) - return as-is, do not re-capture.
        if (txn.getStatus() == TransactionStatus.CAPTURED) {
            return txn;
        }
        return transition(txn, TransactionStatus.CAPTURED);
    }

    @Transactional
    public Transaction refund(String idempotencyKey) {
        Transaction txn = getOrThrow(idempotencyKey);
        if (txn.getStatus() == TransactionStatus.REFUNDED) {
            return txn;
        }
        return transition(txn, TransactionStatus.REFUNDED);
    }

    @Transactional
    public Transaction reverse(String idempotencyKey) {
        Transaction txn = getOrThrow(idempotencyKey);
        if (txn.getStatus() == TransactionStatus.REVERSED) {
            return txn;
        }
        return transition(txn, TransactionStatus.REVERSED);
    }

    @Transactional
    public Transaction fail(String idempotencyKey) {
        Transaction txn = getOrThrow(idempotencyKey);
        if (txn.getStatus() == TransactionStatus.FAILED) {
            return txn;
        }
        return transition(txn, TransactionStatus.FAILED);
    }

    private Transaction transition(Transaction txn, TransactionStatus target) {
        if (!txn.getStatus().canTransitionTo(target)) {
            throw new InvalidStateTransitionException(txn.getStatus(), target);
        }
        txn.setStatus(target);
        txn.setUpdatedAt(Instant.now());
        return transactionRepository.save(txn);
    }

    private Transaction getOrThrow(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalArgumentException("No transaction found for idempotencyKey: " + idempotencyKey));
    }
}
