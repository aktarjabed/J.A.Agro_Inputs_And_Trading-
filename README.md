# J.A. Agro Inputs & Trading

An offline-first Android application designed to manage billing, inventory, and analytics for agricultural input trading securely and reliably on local devices.

## Features

### Core Billing
- **Invoice Creation**: Supports creating comprehensive `TAX INVOICE` and `BILL OF SUPPLY` documents.
- **GST Calculation**: Accurate intra-state and inter-state GST computations, maintaining explicit separation for CGST, SGST, and IGST.
- **GST Compliance Fields**: Supports configuring Place of Supply, Reverse Charge indicators, HSN/SAC codes, and UQC (Unit Quantity Codes).

### Invoice Integrity
- **Immutable Snapshots**: Invoice historical snapshots preserve the exact state at creation. Subsequent edits to products or customers will not distort historical records.
- **Idempotency & Concurrency**: Highly concurrent, robust Room SQLite transactions with fingerprint-based idempotency mechanisms to prevent duplicate bill generation.
- **Invoice Lifecycle**: Invoices support atomic cancellation and explicit state transitions (`COMPLETED` to `CANCELLED`).

### Customer & Ledger Management
- **Customer Master Database**: Integrated directory to store customer details and GSTIN securely.
- **Payment Ledger**: Track partial payments, full payments, and cumulative balance dues for specific invoices sequentially. Reject overpayments and invalid monetary states automatically.

### Inventory Tracking
- **Product Catalog**: Maintain active/inactive products, individual UQC/HSN configurations, and dynamic pricing.
- **Transactional Deductions**: Atomic stock decrement directly linked to invoice completion.
- **Audit Trails**: Every inventory change automatically generates a verifiable `StockMovement` (e.g., `SALE`, `SALE_REVERSAL`) retaining `stockBefore` and `stockAfter` invariants.

### Analytics Dashboard
- Aggregated insights populated natively via pure-SQL Room `Flow` queries (bypassing heavy memory loads).
- Immediate visibility on **Total Revenue**, **Today's Revenue**, **Pending Dues**, active product count, and low-stock alerts.
- Visual **7-Day Revenue** historical chart using deterministic, zero-filled aggregations.

### Reporting & Filtering
- Filter, search, and sort invoices dynamically by number, date range, payment status (`PAID`, `PARTIAL`, `DUE`), document type, or cancellation status.
- Export highly-detailed PDF invoices featuring Indian rupee text-conversion ("Amount in Words"), GST breakdown, itemization, and accurate seller identity headers.

## Architecture

- **100% Offline-First**: Built natively for Android using Jetpack Compose, Kotlin Coroutines, and Hilt Dependency Injection without needing cloud servers.
- **API 36 Ready**: Verified to compile and run against modern Android environments, incorporating `16-KB page-size` compatibility for native libraries.

## Data & Security

- **Encrypted Local Storage**: Leveraging `SQLCipher` to completely encrypt the Room database and protect sensitive business trading information at rest on the mobile device.
- **Tenant Isolation**: Deep multi-business encapsulation ensures multiple profiles can operate entirely separately, guaranteed by rigorous `businessId` checks in every DAO database query.
- **Automated Quota Governance**: Track feature utilization via `QuotaGate`, restricting operations based on tiered device quotas.

## Testing & Project Status

- JVM Unit Tests, Linting, and native Release compilation (`assembleRelease`) are fully verified locally.
- Core financial source-of-truth invariants and migrations (`version 13` through `version 18`) maintain dedicated architectural tests.
- UI/Database Instrumentation (`connectedDebugAndroidTest`) is tested primarily via automated API 36 CI Emulators to guarantee thread-safe runtime safety in a controlled environment.

*(Note: Live government e-Invoice integration/IRN generation is not currently active. E-Invoice metadata fields exist only for manual reference and future expansion.)*

## Screenshots

*Actual runtime application screenshots will be populated natively from CI emulator automation in subsequent releases.*

