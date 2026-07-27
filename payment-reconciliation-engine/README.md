# Payment Reconciliation Engine

A Java/Spring Boot service modeling the core lifecycle of a payment platform:
**authorize → capture → refund / reversal**, plus a **reconciliation job** that
matches internal ledger records against a simulated bank settlement file.

Built to demonstrate the concerns that actually matter in payments engineering —
idempotency under retries, strict state-machine enforcement, and ledger/settlement
matching — rather than another CRUD app.

## Architecture

```
Client / Kafka Producer
        |
        v
 TransactionEvent (Kafka topic: transaction-events)
        |
        v
 TransactionEventListener  --->  PaymentService  --->  TransactionRepository (Postgres)
        |                              |
        |                     TransactionStatus state machine
        |                     (AUTHORIZED -> CAPTURED -> REFUNDED/REVERSED)
        v
 REST API (TransactionController) -- for synchronous authorize/capture/refund calls

 ReconciliationService
   reads CAPTURED/REFUNDED transactions + SettlementRecord batch
   -> produces ReconciliationReport (matched / mismatched / missing)
```

### Idempotency

Every transaction is keyed by a client-supplied `idempotencyKey`, enforced
unique at the database level (`Transaction.idempotencyKey`). Retried
authorize/capture/refund calls with the same key return the existing record
instead of reprocessing — this is what prevents double-charging a customer
when a client retries after a network timeout.

### State machine

`TransactionStatus` encodes legal transitions explicitly:

```
AUTHORIZED -> CAPTURED -> REFUNDED
AUTHORIZED -> CAPTURED -> REVERSED
AUTHORIZED -> REVERSED
AUTHORIZED -> FAILED
```

An illegal transition (e.g. refunding a transaction that was never captured)
throws `InvalidStateTransitionException` instead of silently corrupting the
ledger. This matters most when events arrive out of order over Kafka.

### Reconciliation

`ReconciliationService.reconcile()` compares internal `CAPTURED`/`REFUNDED`
transactions against `SettlementRecord` rows (representing a bank/acquirer
settlement batch) and classifies each as:

- **MATCHED** — amounts agree
- **AMOUNT_MISMATCH** — present on both sides, amounts differ
- **MISSING_IN_SETTLEMENT** — captured internally, never settled (possible processor failure)
- **MISSING_IN_LEDGER** — settled but no internal record (possible duplicate charge/fraud signal)

## Running locally

Default profile uses an in-memory H2 database, so it runs with zero setup:

```bash
mvn spring-boot:run
```

H2 console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:reconciliation`)

To run against real Postgres:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Kafka is expected at `localhost:9092` (see `application.yml`); run without a
broker and only the REST endpoints will be exercised.

## API examples

**Authorize**
```bash
curl -X POST localhost:8080/api/v1/transactions/authorize \
  -H "Content-Type: application/json" \
  -d '{"idempotencyKey":"txn-001","merchantId":"merchant-1","amount":49.99,"currency":"USD"}'
```

**Capture**
```bash
curl -X POST localhost:8080/api/v1/transactions/txn-001/capture
```

**Refund**
```bash
curl -X POST localhost:8080/api/v1/transactions/txn-001/refund
```

**Run reconciliation**
```bash
curl -X POST localhost:8080/api/v1/reconciliation/run
```

## Tests

```bash
mvn test
```

`PaymentServiceTest` covers: duplicate authorize requests being idempotent,
legal transitions succeeding, illegal transitions (refund-before-capture,
reverse-after-refund) throwing, and repeated capture calls not double-processing.

## Tech stack

Java 11 · Spring Boot 2.7 · Spring Data JPA · PostgreSQL (H2 for local dev) ·
Apache Kafka · Maven · JUnit 5 · Mockito · Lombok
