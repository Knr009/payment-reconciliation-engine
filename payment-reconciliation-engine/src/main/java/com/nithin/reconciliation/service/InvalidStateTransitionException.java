package com.nithin.reconciliation.service;

import com.nithin.reconciliation.model.TransactionStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(TransactionStatus from, TransactionStatus to) {
        super(String.format("Illegal transition: cannot move transaction from %s to %s", from, to));
    }
}
