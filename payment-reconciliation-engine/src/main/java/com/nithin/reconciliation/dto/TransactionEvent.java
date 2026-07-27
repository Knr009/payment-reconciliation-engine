package com.nithin.reconciliation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Event published to / consumed from the "transaction-events" Kafka topic.
 * eventType drives which PaymentService method the listener invokes -
 * this is what lets capture/refund/reversal events arrive asynchronously
 * and out of order without corrupting state (illegal transitions are
 * rejected, not silently applied).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String eventType; // AUTHORIZE, CAPTURE, REFUND, REVERSE, FAIL
    private String idempotencyKey;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
}
