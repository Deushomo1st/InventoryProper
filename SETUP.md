# ElectDeptSoftware — Setup Guide

Microservices system for an electrical department: load calculations, circuit
design, protection coordination, cable sizing, energy monitoring, asset
management, work orders, outage management, SCADA integration, GIS mapping,
billing & tariff, regulatory reporting, document management, and auth/permissions.

Built on Spring Boot 3.5.14, PostgreSQL, JWT (JJWT 0.12.6).

---

## What's in the box

**18 services** (each is its own Maven module + Spring Boot app):

| Group | Service | Port | Schema |
|---|---|---|---|
| Platform | ApiGateway | 9000 | — |
| Platform | AuthPermissionsService | 9001 | `auth` |
| Platform | UomService | 9013 | `uom` |
| Platform | AuditService | 9012 | `audit` |
| Platform | NotificationService | 9014 | `notification` |
| Core Electrical | AssetManagementService | 9002 | `asset` |
| Core Electrical | EnergyMonitoringService | 9003 | `energy` |
| Core Electrical | CircuitDesignService | 9008 | `circuit` |
| Core Electrical | LoadCalculationService | 9011 | `loadcalc` |
| Core Electrical | CableSizingService | 9017 | `cable` |
| Core Electrical | ProtectionCoordinationService | 9007 | `protection` |
| Operations | GISMappingService | 9004 | `gis` |
| Operations | WorkOrderService | 9006 | `workorder` |
| Operations | SCADAIntegrationService | 9009 | `scada` |
| Operations | OutageManagementService | 9010 | `outage` |
| Business | BillingTariffService | 9016 | `billing` |
| Business | RegulatoryReportingService | 9015 | `regulatory` |
| Business | DocumentManagementService | 9005 | `document` |

All 18 services share one PostgreSQL database (`ElectDeptSoftware`) with one
schema per service. The ApiGateway routes browser requests to the right
downstream service based on the first URL segment (e.g. `/asset/...` → port 9002).

---

## Fresh-laptop setup

### 1. Install prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK 17 | any 17+ | The pom.xml's `<java.version>17</java.version>` requires it |
| Maven 3.6+ | bundled in IntelliJ | Or install standalone |
| PostgreSQL 14+ | any | Default port 5432, default `postgres` superuser |
| IntelliJ IDEA | Community OK | Or any IDE that handles multi-module Maven |

### 2. Create the database

From psql or pgAdmin, connected as the `postgres` superuser, run **once**:

```bash
psql -U postgres -f bootstrap-schemas.sql
```

This creates the `ElectDeptSoftware` database and all 17 schemas.

### 3. Set the JWT secret

Each service reads `ELECTDEPT_JWT_SECRET` from the environment. Set it once
(any 32+ char random string works):

**Windows (PowerShell):**
```powershell
setx ELECTDEPT_JWT_SECRET "your-very-long-random-secret-here-min-32-chars"
```
Restart IntelliJ after `setx` so it picks up the new env var.

**macOS / Linux:**
```bash
export ELECTDEPT_JWT_SECRET="your-very-long-random-secret-here-min-32-chars"
```
Add to `~/.bashrc` / `~/.zshrc` to persist.

### 4. Open the project in IntelliJ

- File → Open → select the `ElectDeptSoftware` folder
- IntelliJ should detect the 18 Maven modules; if not, right-click the root
  `pom.xml` of each module → "Add as Maven Project"
- Wait for Maven to download dependencies (first time only — Spring Boot,
  PostgreSQL driver, JJWT, etc.)

### 5. Start the services

The recommended order on first launch (so dependent services find what they
need):

1. `ApiGateway` (port 9000) — must be running before anything else
2. `AuthPermissionsService` (port 9001) — login depends on this
3. `UomService`, `AuditService`, `NotificationService` (foundation services)
4. The remaining 14 in any order

Right-click each `*ServiceApplication.java` → Run.

After the first start, Hibernate (`ddl-auto=update`) will have created every
table under its schema. Confirm with:

```sql
\connect ElectDeptSoftware
SELECT schemaname, tablename FROM pg_tables
WHERE schemaname IN (
  'auth','asset','uom','gis','document','energy','workorder',
  'protection','circuit','scada','outage','loadcalc','cable',
  'audit','notification','regulatory','billing'
) ORDER BY schemaname, tablename;
```

### 6. Seed initial data

Once tables exist (after step 5), seed the default roles, permissions,
admin user, electrical units, notification templates, and asset classes:

```bash
psql -U postgres -d ElectDeptSoftware -f bootstrap-seed.sql
```

### 7. Log in

Open http://localhost:9000/ in a browser. Default credentials:

```
Email:    admin@electdept.com
Password: password
```

**⚠️ Change the password immediately** — this is a well-known demo BCrypt
hash. Use:

```
PUT http://localhost:9000/auth/users/{id}
Authorization: Bearer <jwt>
Content-Type: application/json

{ "password": "your-new-strong-password" }
```

Or use the Auth & Permissions service UI to update.

---

## Architecture quick reference

### Cross-service communication

Services talk to each other through the ApiGateway, never directly. The
gateway:

- Strips/validates the JWT
- Routes `/X/**` to the service registered for prefix `X`
- Preserves the `Authorization` header so the downstream sees the same JWT

### Cross-schema foreign keys

There are NO JPA cross-service relations. When `energy.meter_reading.asset_id`
needs to point to `asset.asset.id`, it's stored as a `Long`, not a `@ManyToOne`.
The owning service fetches the related row via REST when needed.

### Hibernate `ddl-auto=update`

This bootstraps tables from entities on first run. Trade-off: Hibernate can
fail to drop columns when entities change (it only adds/never removes).
If you change an entity to drop a field, drop the column manually:

```sql
ALTER TABLE asset.asset DROP COLUMN deprecated_field;
```

### Auth flow

1. Browser POSTs `/auth/login` with `{email, password}` → gateway forwards
   to AuthPermissionsService.
2. Auth service returns `{jwt, user}`. Browser stores JWT in `sessionStorage`.
3. Every subsequent request includes `Authorization: Bearer <jwt>`.
4. Each downstream service has a `JwtAuthFilter` that:
   - Decodes the JWT (using `ELECTDEPT_JWT_SECRET`)
   - Sets `authentication.principal` = `Long userId` (the JWT `sub` claim)
   - Rejects with 401 if missing / invalid / expired

---

## Domain glossary (vs. the InventoryProper template this was forked from)

| InventoryProper | ElectDeptSoftware | What it represents now |
|---|---|---|
| Product | **AssetModel** | A model of equipment (e.g. "ABB VD4 Vacuum Breaker") |
| Sku | **Asset** | An individual deployed unit (e.g. "Breaker #847 at Sub N5") |
| Category | **AssetClass** | Top-level taxonomy (Transformer/Breaker/Cable/…) |
| ProductAttribute | **AssetSpec** | Technical specs for the model (voltage, kVA, IP rating) |
| Warehouse | **Substation** | A primary electrical node |
| Zone | **Feeder** | A distribution circuit out of a substation |
| Bin | **Pole** | An individual pole/structure on a feeder |
| Supplier | **Document** | A managed document (as-built, test report, manual) |
| Contact | **Reviewer** | A person who reviews/approves documents |
| PriceList (in supplier) | **DocumentRevision** | A version of a document |
| StockLedger | **MeterReading** | A timestamped meter reading event |
| StockLevel | **CurrentReading** | The latest reading for a meter |
| PurchaseOrder | **WorkOrder** | A job to dispatch to a field crew |
| PurchaseOrderLine | **WorkOrderTask** | A task within a work order |
| Customer | **ProtectionScheme** | A protection scheme being studied |
| SalesOrder | **CoordinationStudy** | A protection coordination study |
| SalesOrderLine | **DeviceSelection** | A breaker/fuse selection within a study |
| Shipment | **OutageEvent** | An outage event being tracked |
| ShipmentLine | **AffectedAsset** | An asset affected by the outage |
| Movement | **CircuitDiagram** | A single-line diagram |
| MovementLine | **DiagramElement** | A component within a diagram |
| GoodsReceipt | **TelemetryBatch** | A batch of telemetry ingested from SCADA |
| GoodsReceiptLine | **TelemetryReading** | A single telemetry reading |
| PriceList (in pricing) | **TariffSchedule** | A tariff rate schedule (TOU rates, etc.) |
| PriceListEntry | **TariffRate** | A specific rate within a schedule |
| Discount | **RateAdjustment** | A rate adjustment (demand charge, NEM credit, …) |
| Quote | **BillEstimate** | A bill estimate for a customer |
| ReplenishmentRule | **LoadCalcRequest** | A request to run a load calculation |
| ReplenishmentSuggestion | **LoadCalcResult** | The result of a load calculation |

### Role glossary

| InventoryProper role | ElectDeptSoftware role | Who it represents |
|---|---|---|
| ADMIN | **ADMIN** | Full system access |
| OPERATOR (catalog/inventory admin) | **SENIOR_ENGINEER** | Authorizes designs & calculations |
| RECEIVER | **FIELD_TECHNICIAN** | Works on assets, reads meters |
| SHIPPER | **LINE_CREW** | Dispatched to outages |
| PRICING_MANAGER | **TARIFF_MANAGER** | Manages billing & rate adjustments |
| AUDITOR | **COMPLIANCE_OFFICER** | NERC/IEEE/NEPA compliance |
| VIEWER | **CONTRACTOR** | Read-only external party |

---

## Troubleshooting

### `SchemaManagementException: missing table [asset.asset_model]`

Hibernate's `ddl-auto=update` couldn't create the table — usually means the
schema doesn't exist. Run `bootstrap-schemas.sql` again.

### `relation "auth.app_user" does not exist`

The auth service hasn't been started yet (or didn't finish startup). Start
AuthPermissionsService and wait for `Started AuthPermissionsServiceApplication`
in the console.

### Login returns 401 with empty body

Either:
- The bcrypt hash doesn't match the password (did you change it without
  updating the hash?)
- The JWT secret in `ELECTDEPT_JWT_SECRET` doesn't match what the auth
  service signed with (different env var values across IntelliJ runs?)

### `Connection refused: port 5432`

PostgreSQL isn't running. Start the service (`pg_ctl start` /
Services.msc → postgresql-x64).

### `FATAL: password authentication failed for user "postgres"`

The default password in `application.properties` is `postgres`. Either:
- Set your local `postgres` password to `postgres` (dev only — never in prod)
- Or edit each service's `spring.datasource.password=` to match your local pw

---

## Where the original came from

This project was forked from `InventoryProper` (a warehouse inventory
management system) and converted to the electrical domain via a deep rename.
See [PROGRESS.md](PROGRESS.md) for the legacy work log (its terminology is
the InventoryProper terminology — refer to the glossary above to translate).
