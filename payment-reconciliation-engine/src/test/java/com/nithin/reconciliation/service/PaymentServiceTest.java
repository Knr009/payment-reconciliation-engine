package com.nithin.reconciliation.service;

import com.nithin.reconciliation.dto.TransactionRequest;
import com.nithin.reconciliation.model.Transaction;
import com.nithin.reconciliation.model.TransactionStatus;
import com.nithin.reconciliation.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(transactionRepository);
    }

    @Test
    void authorize_duplicateIdempotencyKey_returnsExistingTransactionWithoutCreatingNew() {
        TransactionRequest request = buildRequest("key-123");
        Transaction existing = buildTransaction("key-123", TransactionStatus.AUTHORIZED);

        when(transactionRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        Transaction result = paymentService.authorize(request);

        assertEquals(existing, result);
        verify(transactionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void capture_fromAuthorized_transitionsToCaptured() {
        Transaction txn = buildTransaction("key-456", TransactionStatus.AUTHORIZED);
        when(transactionRepository.findByIdempotencyKey("key-456")).thenReturn(Optional.of(txn));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = paymentService.capture("key-456");

        assertEquals(TransactionStatus.CAPTURED, result.getStatus());
    }

    @Test
    void refund_beforeCapture_throwsInvalidStateTransitionException() {
        Transaction txn = buildTransaction("key-789", TransactionStatus.AUTHORIZED);
        when(transactionRepository.findByIdempotencyKey("key-789")).thenReturn(Optional.of(txn));

        assertThrows(InvalidStateTransitionException.class, () -> paymentService.refund("key-789"));
    }

    @Test
    void capture_calledTwice_isIdempotentAndDoesNotThrow() {
        Transaction txn = buildTransaction("key-999", TransactionStatus.CAPTURED);
        when(transactionRepository.findByIdempotencyKey("key-999")).thenReturn(Optional.of(txn));

        Transaction result = paymentService.capture("key-999");

        assertEquals(TransactionStatus.CAPTURED, result.getStatus());
        verify(transactionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void reverse_afterRefund_throwsInvalidStateTransitionException() {
        Transaction txn = buildTransaction("key-111", TransactionStatus.REFUNDED);
        when(transactionRepository.findByIdempotencyKey("key-111")).thenReturn(Optional.of(txn));

        assertThrows(InvalidStateTransitionException.class, () -> paymentService.reverse("key-111"));
    }

    private TransactionRequest buildRequest(String key) {
        TransactionRequest request = new TransactionRequest();
        request.setIdempotencyKey(key);
        request.setMerchantId("merchant-1");
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");
        return request;
    }

    private Transaction buildTransaction(String key, TransactionStatus status) {
        Instant now = Instant.now();
        return Transaction.builder()
            .id(1L)
            .idempotencyKey(key)
            .merchantId("merchant-1")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status(status)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
