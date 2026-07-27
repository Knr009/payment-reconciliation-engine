package com.nithin.reconciliation.controller;

import com.nithin.reconciliation.dto.ReconciliationReport;
import com.nithin.reconciliation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    /**
     * Triggers reconciliation of all CAPTURED/REFUNDED transactions against
     * the settlement records currently loaded. In production this would be
     * scheduled nightly after the settlement file lands.
     */
    @PostMapping("/run")
    public ResponseEntity<ReconciliationReport> run() {
        return ResponseEntity.ok(reconciliationService.reconcile());
    }
}
