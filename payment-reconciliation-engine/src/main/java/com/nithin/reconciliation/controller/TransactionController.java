package com.nithin.reconciliation.controller;

import com.nithin.reconciliation.dto.TransactionRequest;
import com.nithin.reconciliation.model.Transaction;
import com.nithin.reconciliation.repository.TransactionRepository;
import com.nithin.reconciliation.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/authorize")
    public ResponseEntity<Transaction> authorize(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.authorize(request));
    }

    @PostMapping("/{idempotencyKey}/capture")
    public ResponseEntity<Transaction> capture(@PathVariable String idempotencyKey) {
        return ResponseEntity.ok(paymentService.capture(idempotencyKey));
    }

    @PostMapping("/{idempotencyKey}/refund")
    public ResponseEntity<Transaction> refund(@PathVariable String idempotencyKey) {
        return ResponseEntity.ok(paymentService.refund(idempotencyKey));
    }

    @PostMapping("/{idempotencyKey}/reverse")
    public ResponseEntity<Transaction> reverse(@PathVariable String idempotencyKey) {
        return ResponseEntity.ok(paymentService.reverse(idempotencyKey));
    }

    @GetMapping("/{idempotencyKey}")
    public ResponseEntity<Transaction> get(@PathVariable String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
