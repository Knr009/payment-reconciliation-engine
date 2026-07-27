package com.nithin.reconciliation.service;

import com.nithin.reconciliation.dto.TransactionEvent;
import com.nithin.reconciliation.dto.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes transaction lifecycle events from Kafka. Out-of-order or
 * duplicate delivery (Kafka's at-least-once guarantee) is safe here because
 * PaymentService is idempotent per idempotencyKey and rejects illegal state
 * transitions instead of applying them blindly.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "transaction-events", groupId = "reconciliation-engine")
    public void onTransactionEvent(TransactionEvent event) {
        try {
            switch (event.getEventType()) {
                case "AUTHORIZE":
                    TransactionRequest request = new TransactionRequest();
                    request.setIdempotencyKey(event.getIdempotencyKey());
                    request.setMerchantId(event.getMerchantId());
                    request.setAmount(event.getAmount());
                    request.setCurrency(event.getCurrency());
                    paymentService.authorize(request);
                    break;
                case "CAPTURE":
                    paymentService.capture(event.getIdempotencyKey());
                    break;
                case "REFUND":
                    paymentService.refund(event.getIdempotencyKey());
                    break;
                case "REVERSE":
                    paymentService.reverse(event.getIdempotencyKey());
                    break;
                case "FAIL":
                    paymentService.fail(event.getIdempotencyKey());
                    break;
                default:
                    log.warn("Unknown event type received: {}", event.getEventType());
            }
        } catch (InvalidStateTransitionException e) {
            // Out-of-order event (e.g. REFUND arriving before CAPTURE due to
            // consumer lag). Logged, not thrown, so one bad event doesn't
            // block the partition.
            log.warn("Rejected out-of-order event for key {}: {}", event.getIdempotencyKey(), e.getMessage());
        }
    }
}
