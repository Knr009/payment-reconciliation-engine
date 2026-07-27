package com.nithin.reconciliation.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Payment lifecycle states and the state machine that governs legal transitions.
 * Mirrors real payment processor semantics: AUTHORIZED -> CAPTURED -> (REFUNDED | REVERSED),
 * with FAILED reachable from AUTHORIZED only.
 */
public enum TransactionStatus {
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    REVERSED,
    FAILED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TransactionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(AUTHORIZED, EnumSet.of(CAPTURED, REVERSED, FAILED));
        ALLOWED_TRANSITIONS.put(CAPTURED, EnumSet.of(REFUNDED, REVERSED));
        ALLOWED_TRANSITIONS.put(REFUNDED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(REVERSED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(FAILED, EnumSet.noneOf(TransactionStatus.class));
    }

    public boolean canTransitionTo(TransactionStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(TransactionStatus.class)).contains(target);
    }
}
