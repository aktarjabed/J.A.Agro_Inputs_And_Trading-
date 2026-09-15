# INBusiness - Agro Inputs Invoicing & Financial Management

INBusiness is an offline-first Android application designed specifically for **J.A. Agro Inputs & Trading**. It provides a robust, encrypted, transactional framework for generating invoices, tracking stock, receiving payments, and managing customer ledgers. It supports standard accounting invariants and complies with regional tax/GST formatting without live IRP integration.

## Current Production Capabilities

- **Offline-First:** All data operations are local, powered by Room SQLite.
- **Security:** Entire database is fully encrypted with **SQLCipher** for Android. Keys are managed by Android Keystore. The database is strictly excluded from Android cloud backups for structural safety.
- **Data Isolation:** Fully scoped architecture supporting multiple businesses internally. Transactions strictly validate the `businessId`.

## Implemented Modules

### Transactional Ledger (Phase 3 & 4)
- **Invoice Lifecycle:** Explicit `status` tracking (e.g., `COMPLETED`, `CANCELLED`). Support for `TAX_INVOICE`, `BILL_OF_SUPPLY`. Atomic transactional creation.
- **Stock Movements:** Every inventory update enforces double-entry rules. Support for `SALE`, `SALE_REVERSAL`, etc. Editing products does not bypass movements.
- **Payment Invariants:** `amountPaid` and `balanceDue` strictly track multiple ledger payments (`amountPaid == SUM(valid Payment.amount)`).
- **Cancellation:** Invoices can be cancelled exactly once, producing deterministic `SALE_REVERSAL` records.
- **Idempotency:** Replaying an identical invoice creation request generates no new Side Effects.

### Dashboard & Analytics (Phase 5)
- **SQL-Backed:** In-database aggregation to prevent N+1 and unbounded memory allocations.
- **Timezone Aware:** Relies on the `Asia/Kolkata` timezone internally for accurate day/month/rollover limits. Interval handling uses safe `[start, end)` SQL queries.
- **7-Day Chart:** Always shows exactly the last 7 calendar days, zero-filling empty spots deterministically.
- **Insights:** Total Revenue, Today's Revenue, Pending Dues, Active Products, Low Stock.

### Immutable PDF Generation (Phase 6)
- **Truth at Transaction Time:** PDFs are generated based strictly on immutable snapshots recorded at the time of invoice creation (prices, GST, items, HSN/SAC, UQC, seller/buyer details).
- **Semantically Accurate:** Cumulative payment fields accurately reflect `TOTAL AMOUNT PAID` rather than incorrectly claiming payments were received "today".
- *Note:* No live IRP, E-Invoice, or dynamic QR generation is claimed.

## Technical Architecture

- **API Level:** Targets Android API 36 / SDK 36.
- **Tooling:** Kotlin 1.9+, Android Gradle Plugin 8.6.0.
- **UI:** 100% Jetpack Compose.
- **DI:** Hilt.
- **Concurrency:** Kotlin Coroutines & Flow.

## Missing / Future Implementation

- **Data Backup / Restore:** Currently excluded from automatic backup. A custom, manual secure export feature is required for business continuity.
- **E-Invoice API:** Nullable placeholders exist in the schema, but live submission is disabled.

