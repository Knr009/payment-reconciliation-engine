package com.nithin.reconciliation.repository;

import com.nithin.reconciliation.model.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {
    Optional<SettlementRecord> findByIdempotencyKey(String idempotencyKey);
}
