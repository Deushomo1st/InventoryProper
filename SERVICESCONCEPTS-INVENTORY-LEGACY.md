# ElectDeptSoftware — Services in Plain English

> Each service explained the way you'd explain it to a warehouse manager who's never seen the code: what it does, what it owns, who depends on it. Use this when you (or an AI) forget *why* a service exists. For the build status of each, see `PROGRESS.md`. For schema details, see the PDFs.

---

## Tier 0 — Platform Foundation

### ApiGateway (port 9000)

**ApiGateway = "The front door."** Doesn't own any data. Doesn't know your products, your warehouses, your stock. Its only job is to be the single URL the browser talks to (`localhost:9000`) and quietly forward each request to the right microservice behind it.

Three things it does:
1. **Serves the public pages** — `/login.html`, `/menu.html`, the shared `/css` and `/js`. So you can log in and pick a service without any other service being up.
2. **Routes domain calls** — when the browser asks for `/catalog/products`, the gateway proxies that to CatalogService on port 9002. When it asks for `/inventory/levels`, it proxies to InventoryService on 9003. Etc. Same for `/auth/me`, `/warehouse/bins`, all of it.
3. **Health-checks the tiles** — `/menu.html` pings `/health/{service}` every 2s; the gateway proxies those pings to each service's `/health` endpoint, so the dashboard tiles glow green or red.

Why it matters: without the gateway, every page would need to know the port of every service. With it, the browser only ever knows port 9000. Services can move ports without breaking the UI.

Doesn't store anything. Doesn't issue JWTs. Doesn't even validate them deeply — it has a JwtAuthFilter for its own protected paths (`/menu.html`), but the *real* token check happens at the destination service.

---

### AuthService (port 9001)

**AuthService = "Who you are, and what you're allowed to do."** Everything in the system traces identity and permission back here.

Four things it owns:
1. **Users** (`auth.app_user`) — email, password hash, active flag, manager flag, last-seen timestamp. The thing you log in as.
2. **Roles** (`auth.role`) — named buckets of permissions, like `BUYER`, `WAREHOUSE_OPS`, `ADMIN`.
3. **Permissions** (`auth.permission`) — fine-grained verbs, like `po.create`, `po.approve`, `stock.read`. Code format is always `dot.namespace`.
4. **Substation access** (`auth.warehouse_access`) — which warehouses a user can even *see* the contents of.

The login flow: you POST email + password. AuthService checks bcrypt, builds a JWT with claims `{sub, email, roles, perms, whs, manager, exp}`, signs it with `ELECTDEPT_JWT_SECRET`, returns it. The browser stores it in `sessionStorage` and sends it on every subsequent request.

**Critical detail:** AuthService doesn't get called per-request to check permissions. The JWT *carries* the permission list. Every other service validates the token's signature locally and reads the claims. That's why changing a user's roles requires them to log out and back in — the new permissions only appear in a newly-issued token.

Also does the heartbeat: `/auth/me` is hit every 60s by every active tab, which bumps `last_seen_at`. That powers the green/red online dots and the single-session enforcement (if you try to log in while another tab is heartbeating, you get rejected with "already signed in elsewhere").

UI: Users, Roles, Permissions, Substation Access. Manager-only mutations (create/delete/role-assign). View-only for everyone else.

---

## Tier 1 — Master Data

### CatalogService (port 9002)

**CatalogService = "What we sell."** Doesn't know quantities, prices, or suppliers — just *what items exist*.

Four things:
1. **AssetClass** — hierarchical folders. "Beverages → Soft Drinks → Cola." Self-FK on `parent_id`.
2. **AssetModel** — the *concept* of an item. "Coca-Cola Classic," "Levi's 501 Jeans." Has auto-generated `PROD-NNNN` code, brand, status, dimensions, one parent category.
3. **SKU** — the *atomic sellable variant*. A product with no SKUs is useless because you can't buy "a Coca-Cola" — you buy a `12oz Can` or a `2L Bottle`. Each SKU has a unit-of-measure (FK to UomService).
4. **AssetSpec** — flexible key-value extras tied to a product. Concrete row: `(product_id=42, key="RAM", value="16GB")` for a laptop. Same table also stores `(key="Storage", value="512GB SSD")` and `(key="Battery", value="8 hours")` for that same laptop, plus `(key="Caffeine", value="34mg")` for a can of cola, `(key="Material", value="Velvet")` for a sofa, `(key="Allergens", value="Gluten, Dairy")` for a frozen pizza. Adding a new kind of spec = insert another row, no schema change.

**Why product vs SKU matters everywhere:** Inventory counts SKUs, not products. Suppliers price SKUs. Sales orders pick SKUs. You never have "100 Coca-Colas in warehouse" — you have "100 cans + 50 bottles + 20 two-liters." Every higher tier of the system depends on the catalog spine.

**Why attributes are a separate table, not columns:** If every product type had its own columns (`ram`, `caffeine_mg`, `material`, `allergens`, `sterilization_class`...) the `product` table would be 200 columns of mostly-NULLs. Key-value attribute rows let you start selling laptops, then sofas, then frozen pizzas, then medical equipment, without ever altering the schema. The trade-off: querying is harder — "all laptops with ≥16GB RAM" becomes a self-join on `product_attribute` plus a string-to-number parse, vs the clean `WHERE ram_gb >= 16` you'd get with a real column. Rule of thumb: properties that define *what the product is* (name, brand, category, status) → real columns. Specs and metadata that vary by product type → attribute rows. Once a spec stabilizes across enough products (every laptop has RAM/Storage/CPU), it may be worth promoting to a typed sub-table.

UI: Categories tree, Products (with search + bulk-create), SKUs (variants per product), Attributes (pick a product, add `key`/`value` rows freely).

---

### UomService (port 9013)

**UomService = "How we measure things."** Tiny but essential. Without it, "100" is meaningless — 100 what?

Two things:
1. **UoM** — units. `EA` (each), `KG` (kilogram), `L` (liter), `M` (meter), `BOX`, `PALLET`.
2. **UomConversion** — how to convert between two units. `1 BOX = 24 EA`. `1 PALLET = 50 BOX`. `1 KG = 1000 G`.

Why it matters: a SKU's UoM is set in the catalog (e.g., the 12oz Can is `EA`). But the *supplier* might price it per `BOX` ("$10 per box of 24"). When Procurement creates a purchase order, it has to convert: 5 boxes ordered = 120 cans receivable. UomService is the rulebook for those conversions.

Used heavily by Procurement, Receiving, Pricing, Reporting. Used a little by Inventory (when an adjustment is entered in a different UoM than the SKU's native one).

UI: list of units, conversion matrix. Lightweight CRUD.

---

### WarehouseService (port 9004)

**WarehouseService = "Where stuff lives."** The physical map of your inventory operation.

Three-level hierarchy:
1. **Substation** — a building. `WH-MAIN`, `WH-NORTH`. Has a code, name, address.
2. **Feeder** — a section within a warehouse, by purpose. `RECEIVING`, `BULK`, `PICK_FACE`, `COLD_STORAGE`. Each zone belongs to one warehouse.
3. **Pole** — a specific slot within a zone. `A-12-03` (aisle A, rack 12, level 3). Each bin belongs to one zone.

**Why three levels?** Real warehouses do this. You don't say "I have 100 cans in Substation North" — you say "100 cans in Pole A-12-03, which is in Feeder BULK, which is in WH-NORTH." This precision matters when:
- A receiver puts away stock (which bin?)
- A picker fulfills an order (where to grab it from?)
- An auditor counts (which bins to scan?)
- A replenisher moves stock (from BULK to PICK_FACE).

Inventory stores `(sku_id, warehouse_id, bin_id)` triples — that's why the Stock Levels modal needs both a Substation dropdown *and* a Pole dropdown that filters by the chosen warehouse.

Also gates *who can see what* — `auth.warehouse_access` ties users to specific warehouses. A buyer for the North region shouldn't see Main's stock.

UI: Warehouses, Zones (per warehouse), Bins (per zone).

---

### SupplierService (port 9005)

**SupplierService = "Who we buy from."** The vendor side of the business.

Three things:
1. **Document** — the company you buy from. Code, name, status, payment terms ("Net 30", "Prepaid").
2. **SupplierContact** — humans at the supplier. Name, email, phone, role ("Sales Rep," "Billing").
3. **SupplierPriceList** — per-SKU pricing from this supplier. `sku_id` is a cross-schema FK to `catalog.sku`. Includes unit price, currency, valid-from/valid-to dates, active flag.

**Why pricelists are per-SKU:** Acme Beverages might sell you `Coca-Cola 12oz Can` at $0.40/each *and* `Coca-Cola 2L Bottle` at $1.20/each — two rows in the pricelist, same supplier, different SKUs. When Procurement creates a PO, it picks a supplier and gets the right price for each line.

Also handles *date-bounded* pricing. A supplier might say "this price is good until March 31." After that date, the row is `valid_to` in the past and the system stops using it.

UI: Suppliers, Contacts (per supplier), PriceLists (cross-tabulated by supplier + SKU).

---

## Tier 2 — State

### InventoryService (port 9003)

**InventoryService = "How much we have, and the full history of every change."** The single source of truth for stock quantities.

Two things, working together:

1. **CurrentReading** — current cache. One row per `(sku, warehouse, bin)` triple, holding the current `qty_on_hand`. This is what the UI reads. Read-fast, write-only-by-the-ledger.

2. **MeterReading** — append-only audit log. *Every* quantity change writes a ledger row. `delta` is signed (+100 for a receipt, -50 for a consumption). Has a `reason_code` (RECEIPT, ISSUE, ADJUSTMENT, DAMAGE, EXPIRY, COUNT), an optional reference to a source document (`ref_doc_type` + `ref_doc_id` — e.g., the PO that triggered the receipt), and the `user_id` of whoever did it.

**The rule (from the PDF):** *Stock ledger is the only writer of quantity changes.* Other services don't update `qty_on_hand` directly. They post an adjustment, which writes a ledger row and updates the level cache *in one transaction*. If anyone bypasses this, the audit trail breaks.

This is what makes the demo story work. When the auditor asks "who took 50 cans out of bin A-12-03 last Tuesday?" — the ledger has the row, the row has the `user_id`, the `user_id` joins to `auth.app_user`, and you have a name. No "lost data," no "we'll have to ask around."

UI: Overview, Stock Levels (with +NEW ADJUSTMENT modal pulling SKUs from catalog and bins from warehouse), Ledger (read-only audit log with reason and "by whom" columns).

> **Forward note:** the PDF lists `stock-ledger` as a *separate* service in some places. For now both entities live inside InventoryService because they're tightly coupled and always written together. If/when the volume justifies, the ledger can be extracted into its own port-9003-adjacent service that Inventory writes to via REST or async events.

---

## Tier 3 — Workflows

### ProcurementService (port 9006) — not yet built

**ProcurementService = "Buying."** The workflow from "we need to buy more cans" to "PO sent to supplier."

Roughly owns:
- **WorkOrder** — header with supplier, ship-to warehouse, status (DRAFT, SUBMITTED, APPROVED, RECEIVED, CLOSED).
- **WorkOrderTask** — one per SKU + quantity + unit price (from the supplier's pricelist at time of creation).
- **Approval** — who approved which PO, when. FK to `auth.app_user.id` — that's the auditor's link.

Approval thresholds matter here. A buyer with `po.create` can draft any PO. Only a user with `po.approve` can sign off, and approval rules might depend on dollar value, warehouse, or category.

When a PO is APPROVED and the goods arrive, **ReceivingService** is what actually moves stock into inventory (writing a ledger row with `ref_doc_type=PO`, `ref_doc_id=<po_id>`).

UI vision: PO list, PO detail with line items, Approvals queue for managers.

---

### SalesService (port 9007) — not yet built

**SalesService = "Selling."** The mirror of Procurement, but customer-facing.

Owns:
- **CoordinationStudy** — header with customer, ship-from warehouse, status (DRAFT, CONFIRMED, PICKING, SHIPPED, INVOICED).
- **DeviceSelection** — one per SKU + quantity + price (from PricingService when it lands).
- **ProtectionScheme** — the people buying from us. Code, name, address, payment terms, credit limit.

When a sales order is confirmed, **FulfillmentService** picks the goods out of inventory (writing a ledger row with `delta` negative, `ref_doc_type=SO`).

UI vision: Customers, Sales Orders, Order detail with status timeline.

---

### ReceivingService (port 9009) — not yet built

**ReceivingService = "Stuff arrived. Put it away."** The "in" door of the warehouse.

Workflow:
1. PO is APPROVED, supplier ships goods.
2. Truck arrives at the dock. Receiver opens the PO in Receiving's UI.
3. Receiver scans/counts what actually arrived (might not match the PO — 100 ordered, 98 received).
4. Receiver picks a bin (usually in the `RECEIVING` zone first, then moved to `BULK` later by CircuitDiagram).
5. Receiving posts an adjustment to InventoryService: `+98` for that SKU in that bin, reason `RECEIPT`, ref to the PO.
6. The PO line is marked as received.

Owns: **TelemetryBatch** (header), **TelemetryReading** (per SKU received, vs ordered).

UI vision: Open POs awaiting receipt, scan-in screen, putaway suggestions.

---

### FulfillmentService (port 9010) — not yet built

**FulfillmentService = "Order's been placed. Pick, pack, ship."** The "out" door.

Workflow:
1. Sales order is CONFIRMED.
2. System generates a **PickList** — which bin to grab from for each line. Usually pulls from `PICK_FACE` zone (small quantities, accessible) before `BULK` (pallets).
3. Picker walks the list, scans each SKU as picked.
4. Packer boxes the order, prints a label.
5. Shipper hands off to carrier (UPS, FedEx, etc.).
6. Each pick writes a negative ledger row: `-1`, reason `ISSUE`, ref to the SO.

Owns: **PickList**, **PickListLine**, **OutageEvent**, **ShipmentTracking**.

UI vision: Pick queue, packing station, shipping manifest.

---

### MovementService (port 9008) — not yet built

**MovementService = "Stock changing locations within our walls."** Doesn't change *how much* you have, just *where*.

Two flavors:
1. **Intra-warehouse moves** — move 50 cans from BULK bin A-12-03 to PICK_FACE bin P-05-02. Net inventory change is zero, but two ledger rows are written: -50 in source bin, +50 in destination bin, reason `MOVE`.
2. **Inter-warehouse transfers** — move 200 cans from WH-MAIN to WH-NORTH. Logically the same, but typically goes through an in-transit state for the truck.

Why a separate service? CircuitDiagram is a *workflow* with steps (suggest a move, approve, execute, confirm). And it's high-volume — every replenishment from BULK to PICK_FACE is a movement.

Owns: **MovementOrder**, **MovementOrderLine**.

UI vision: Pending moves, executor screen, completed history.

---

### AuditService (port 9012) — not yet built

**AuditService = "Who did what, system-wide."** Different from the stock ledger — this is *every action*, not just quantity changes.

Listens (or polls) for activity events across all services and writes them to a unified log:
- "User 7 approved PO-44 at 14:32"
- "User 12 created AssetModel PROD-0089 at 09:15"
- "User 3 changed Role BUYER's permissions at 11:08"
- "User 7 logged in from IP 192.168.1.5 at 08:00"

Owns: **AuditEvent** — id, user_id, event_type, resource_type, resource_id, metadata (jsonb), timestamp.

Why separate from the stock ledger? Stock ledger is *operational* — used to reconstruct inventory at any point in time. Audit log is *forensic* — used when a compliance officer or security team asks "show me everything user 7 did in the last 30 days."

UI vision: Search by user, by event type, by date range. Timeline view.

> Note: this is the same scope as the "Track button" backlog item in PROGRESS.md section 9.

---

## Tier 4 — Intelligence

### ReplenishmentService (port 9011) — not yet built

**ReplenishmentService = "Notice we're running low and do something about it."** The brain that prevents stockouts.

For each SKU, knows two thresholds:
- **Reorder point** — when stock drops below this, time to reorder.
- **Reorder quantity** — how much to order each time.

Polls (or subscribes to) stock-level changes. When it sees `qty_on_hand < reorder_point` for any SKU:
1. Picks a supplier (cheapest from SupplierService's pricelist, or preferred-vendor logic).
2. Creates a draft PO in ProcurementService.
3. Notifies the buyer via NotificationService.

Also handles **PICK_FACE replenishment** — when a pick-face bin runs low, trigger a movement from BULK to top it up. Different path, same logic.

Owns: **ReplenishmentRule** (per SKU + warehouse) and a queue of pending replenishment suggestions.

UI vision: Rule editor, suggestion queue, override controls.

---

### PricingService (port 9016) — not yet built

**PricingService = "How much to charge."** The customer-side mirror of Document pricelists.

Owns:
- **PriceList** — per-customer or per-tier base pricing. "Wholesale tier 1," "Retail," "Internal."
- **TariffRate** — SKU + unit price + currency + valid-from/valid-to.
- **RateAdjustment** — promo rules. "10% off all beverages this week," "$5 off if you buy 3+."
- **TaxRule** — sales tax by region.

When SalesService creates a sales order line, it calls PricingService: "for this customer, this SKU, this quantity, this date — what's the price?" PricingService returns the answer (base price - discounts + tax). The result is stamped on the order line so it's stable.

UI vision: Pricelists, discount rules, tax matrix.

---

### NotificationService (port 9014) — not yet built

**NotificationService = "Tell humans things happened."** The outbound communication layer.

Channels: email (SMTP), SMS (Twilio-ish), in-app banner, eventually push.

Listens for events:
- "PO-44 needs your approval" → email to managers with `po.approve`
- "Stock is low on SKU-1234" → email to the replenishment buyer
- "OutageEvent SH-99 delivered" → email to customer
- "Your password was changed" → email to user

Owns: **NotificationTemplate** (the parameterized message body per event type), **NotificationLog** (every message sent, status, failures).

Doesn't *decide* when to notify — other services emit events. Notification just translates them to messages and delivers.

UI vision: Templates editor, delivery log, channel config.

---

### ReportingService (port 9015) — not yet built

**ReportingService = "Dashboards and exports."** The "show me a chart" layer.

Read-only across the whole system. Aggregates data from every other schema and presents:
- Stock-on-hand by warehouse / category / age
- Receipts and shipments over time
- Open POs by supplier, days outstanding
- Top-selling SKUs by revenue or volume
- Stockout incidents
- User activity summary

Doesn't own much data of its own — maybe **ReportTemplate** (saved queries) and **ScheduledReport** (auto-email this dashboard to the CFO every Monday at 8am).

UI vision: Pre-built dashboards, ad-hoc query builder, CSV/PDF export.

---

## (Future) Eureka — port 9017

**Eureka = "Service phonebook."** Not yet built. Currently the gateway hardcodes service URLs in `application.properties` (`services.catalog.url=http://localhost:9002`, etc.). When Eureka lands, every service registers itself by name on startup, and the gateway looks up "catalog" instead of remembering its port. Makes it easier to run multiple instances of a service for load-balancing, move services between hosts, or spin up dev environments.

Until then: hardcoded URLs are fine. Eureka is a Tier-0 retrofit, not a blocker for anything in Tiers 1–4.

---

## How they connect (one-paragraph version)

A buyer **logs in** (AuthService issues JWT). They **browse products** (CatalogService) and find a low-stock SKU (current quantity from InventoryService). They **create a PO** (ProcurementService) picking a supplier (SupplierService) at a price (SupplierService pricelist) for delivery to a warehouse (WarehouseService). A manager **approves the PO** (ProcurementService, gated by `po.approve` from the JWT). The truck arrives, **goods are received** (ReceivingService posts an adjustment to InventoryService, which writes a stock-ledger row referencing the PO, signed with the receiver's user_id). Later, a customer order **picks the goods** (FulfillmentService, another negative ledger row, referencing the SO). The CFO **opens a dashboard** (ReportingService) showing PO throughput by supplier, joining `procurement.purchase_order` with `auth.app_user` to show who approved what. The whole chain — login → JWT → JwtAuthFilter per service → permission check → operation → FK-backed audit row — is the security and traceability story.

Every connection above is an HTTP call mediated by the gateway, every JWT is validated locally by each service, and every write captures a user_id. That's the design.

---

*Read PROGRESS.md for build status and burn marks. Read the PDFs (`ElectDeptSoftware-Authentication.pdf`, `ElectDeptSoftware-Entities.pdf`, `ElectDeptSoftware-Table-Connections.pdf`, `read heirarchy.pdf`) for the full spec. This file is the "warehouse manager's tour" — start here when the architecture diagram has too many boxes.*
