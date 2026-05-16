# Inventory-Deus — Project Brief & Progress Log

> **You are an AI joining an in-progress project.** Read this file end-to-end before writing any code. It is the single source of truth for *what we are building, why, what is done, what is next, and how to do it like the rest of the codebase.* The architecture PDFs are deeper references; this document is the runtime onboarding.

---

## 1. The Vision

**Inventory-Deus** is a microservices-based inventory management system built in Spring Boot. It models a real warehouse-operations stack — purchase orders, stock movements, sales orders, fulfillment, audit trails — split across **16 services** plus a gateway, an auth-service, and (eventually) Eureka for service discovery. It is a class project, but the design is deliberately faithful to how a real distributed system would be built: per-service schemas, JWT-based stateless auth, no shared compile-time code, a thin gateway, and every operational write tied back to an authenticated user.

**Demo story:** Sam, a buyer with role `BUYER` and access to warehouses 1 and 3, logs in via the portal, opens the procurement dashboard, approves PO-44 (ship-to warehouse 3, worth $2.5M). The approval row in `procurement.approval` records `approver_id = 7` — the same `id` that lives in `auth.app_user`. Hours later an auditor joins `procurement.approval` with `auth.app_user` and proves who did what. The chain — login → JWT → verified per service → permission check → warehouse scope check → FK-backed audit row — is enforced at five separate layers (bcrypt, HMAC, Spring Security, app code, Postgres FK).

**Why this matters even for a class project:** the grader can run one user journey end-to-end and see that no link in the security chain is "trust the client" or "remember to check this." That is the design's selling point.

---

## 2. Architecture at a Glance

```
                          Browser (HTML/CSS/JS, no framework, sessionStorage)
                                          │
                                          ▼
                          ┌───────────────────────────────┐
                          │   ApiGateway   :9000          │
                          │   - serves /login, /menu, /ui │
                          │   - JwtAuthFilter             │
                          │   - RouteController proxies   │
                          └───────────────────────────────┘
                                          │
              ┌───────────┬───────────────┼──────────────┬──────────────┐
              ▼           ▼               ▼              ▼              ▼
        Auth :9001   Catalog :9002   UoM :9013     Warehouse :9004   ... +12 more
        (DONE)       (Tier 1)        (Tier 1)      (Tier 1)
        +HealthCtl   +HealthCtl      +HealthCtl    +HealthCtl
        +JwtFilter   +JwtFilter      +JwtFilter    +JwtFilter
```

**Tiers (build in strict order — see `read heirarchy.pdf`):**

| Tier | Purpose | Services | Status |
|------|---------|----------|--------|
| 0 | Platform foundation | gateway, auth, (eureka) | **DONE** (eureka not yet built) |
| 1 | Master data | catalog, uom, warehouse, supplier | Not started |
| 2 | State | inventory, stock-ledger | Not started |
| 3 | Workflows | procurement, sales, receiving, fulfillment, movement, audit | Not started |
| 4 | Intelligence | replenishment, pricing, notification, reporting | Not started |

**Ports:** sequential 9000–9017. Catalog 9002, Inventory 9003, Warehouse 9004, Supplier 9005, Procurement 9006, Sales 9007, Movement 9008, Receiving 9009, Fulfillment 9010, Replenishment 9011, Audit 9012, UoM 9013, Notification 9014, Reporting 9015, Pricing 9016, Eureka 9017.

---

## 3. Critical Design Rules (do not violate)

1. **Per-service JWT verification.** Every downstream service has its own `JwtAuthFilter` wired into `SecurityConfig` via `.addFilterBefore(...)`. Services do **not** trust the gateway; they trust the JWT signature. Shared secret via `INVENTORY_JWT_SECRET` env var, never hardcoded.
2. **Dual-access mode is mandatory.** Every service must be runnable through the gateway (`localhost:9000/ui/{service}/...`) *and* directly on its own port (`localhost:9001/...`, etc.) — using the *same HTML files unchanged*. `sessionStorage` is per-origin, so each origin gets its own session naturally. All HTML paths are absolute (`/js/...`, `/css/...`, `/login.html`) and resolve against whichever server is serving the page.
3. **Each service has byte-identical copies** of `static/login.html`, `static/js/app.js`, `static/css/style.css` mirroring the gateway's. These are **intentional duplicates** for dual-mode. Do not delete them as "dead code" — that was a mistake made before.
4. **Plain HTML/CSS/JS only.** No framework. No build step. No npm. No TypeScript. No bundler.
5. **UTF-8 forced** in every service's `application.properties` (`server.servlet.encoding.charset=UTF-8`, `enabled=true`, `force=true`) — Spring's static handler defaults drop the charset and `×` / `@` mojibake.
6. **Permissions live in JWT claims** (`perms`), computed at login. No service calls auth-service per request. Changing a role's permissions requires the affected user to log out and back in.
7. **401 ≠ 403.** 401 = "I don't know who you are" → client bounces to `/login.html`. 403 = "I know you, you are not allowed" → client shows an error toast. Never collapse them.
8. **Schema per service** in one Postgres database (`inventory_deus`). Cross-schema FKs are allowed and enforced by Postgres. Only `auth-service` writes to the `auth` schema; only `procurement-service` to `procurement`, etc.
9. **Every operational write captures `user_id`** from JWT subject. No anonymous writes. Audit row in `inventory.stock_ledger`, `procurement.approval`, etc. is FK-backed by `auth.app_user.id`.
10. **Stock ledger is the only writer of quantity changes.** Other services emit events; the ledger applies them.
11. **Eureka registration ≠ authentication.** A service appearing in Eureka is not trusted just because it registered.
12. **Tokens are short-lived** (1 hour, `security.jwt.expiry-minutes=60`). No refresh token in v1 — re-login on expiry is acceptable.
13. **Tab close = logout.** Token is in `sessionStorage`, not `localStorage`, deliberately.
14. **No CLAUDE.md, README.md or other docs files** unless explicitly requested. PROGRESS.md (this file) and `project-context.txt` are the only persistent docs.

---

## 4. Tech Stack

- **Java 17**
- **Spring Boot 3.5.x**
- **Spring Security** with stateless JWT filters (one per service)
- **JJWT 0.12.6** for token creation / verification
- **Spring Data JPA** + **Hibernate** for persistence
- **PostgreSQL 16+** — single database `inventory_deus`, schema per service
- **BCrypt** for password hashing
- **Plain HTML + CSS + JS** for the frontend (no framework, no build step)
- **sessionStorage** for client-side token (keys: `inv_jwt`, `inv_user`)
- **Google Fonts** — Syne (display/sans) + Space Mono (mono/inputs/labels)

---

## 5. The Front-End Pattern

Theme is **dark/techy**. Tokens defined in `:root` of `style.css`:

```
--bg: #0a0a0f          (page background)
--surface: #111118     (cards, topbar)
--surface2: #1a1a24    (hover row, secondary)
--border: #2a2a3a      (subtle borders)
--accent: #00ff88      (green — the glow)
--accent2: #7b61ff     (purple — login gradient accent)
--danger: #ff4466      (pink-red — logout, modal-danger bar)
--warn: #f59e0b        (amber — hint banners)
--text: #e8e8f0
--muted: #6b6b80
--mono: 'Space Mono'
--sans: 'Syne'
```

**Per-page contract for any service UI:**

1. Page is at `static/ui/{service-name}/{page}.html` on its owning service.
2. Page links `/css/style.css` and `/js/app.js` (absolute, resolves to whichever server serves the page).
3. First `<script>` block calls `renderAppShell({ service, page, tabs: [...] })` — injects the consistent header (back-to-menu, breadcrumb, tabs, user email, logout) and starts the presence heartbeat.
4. Page body uses themed classes: `.page`, `.toolbar` (with inner `.actions`), `.data-table`, `.modal-back` wrapper + `.modal` inner + `.modal-x` + `.modal-tag` + `.modal-title` + `.modal-actions`, `.btn-accent`, `.btn-ghost`, `.btn-danger`, `.action-link`, `.action-link.danger`, `.pill`, `.form-group`, `.form-row`, `.perm-group`, `.modal-hint`.
5. Delete confirmations use `confirmDanger({ title, body, confirmLabel, onConfirm })` from `app.js` — **never** call `window.confirm()`.
6. `apiFetch()` is the only way to call APIs (attaches Bearer token, auto-bounces to login on 401).

**Reference page to mimic when building anything new:** `AuthService/src/main/resources/static/ui/auth-service/users.html`. It exercises the full pattern — table, create/edit modal, role-assignment sub-modal, themed delete confirm, disabled action button.

---

## 6. Service Inventory

### 6.1 ApiGateway (port 9000) — DONE

**Java:**
- `config/SecurityConfig.java` — permits `/login.html`, `/menu.html`, `/css/**`, `/js/**`, `/auth/login`, `/auth/register`, `/health/**`, `/ui/**`. Wires `JwtAuthFilter`. Stateless. CSRF disabled.
- `filter/JwtAuthFilter.java` — validates HMAC signature + `exp`, populates `SecurityContext` with `userId` principal + role authorities.
- `util/JwtUtil.java` — `parseToken`, `validateToken`, `getUserId`, `getEmail`, `getRoles`, `getPermissions`, `getWarehouses`.
- `controller/RouteController.java`:
  - `GET /` → redirect to `/login.html`
  - `GET /health/{serviceName}` → proxy to `localhost:<port>/health` with 1s connect / 2s read timeout
  - `POST /auth/login`, `POST /auth/register` → proxy to auth-service (specialized — public, no Auth header)
  - `* /auth/**` → proxy authenticated calls to auth-service with Authorization header forwarded
  - `* /ui/{service}/**` → proxy to downstream services preserving the full URI (does **not** strip the prefix — that was a bug, now fixed)
  - All forwarders catch `HttpClientErrorException`/`HttpServerErrorException` → forward downstream status + body intact; catch `ResourceAccessException` → return 503 with `{"message": "...not reachable..."}`. Prevents Spring's default 500-with-empty-body wrap.
- `RestTemplate` is timeout-configured (1s connect, 2s read) via `SimpleClientHttpRequestFactory`.

**Static:**
- `login.html` — email/password form, eye toggle, defensive JSON parsing, POSTs to `/auth/login`, stores `inv_jwt` + `inv_user`, redirects to `/menu.html`.
- `menu.html` — 16-tile dashboard, pings `/health/{id}` every 2s, opens "service offline" modal on click of a down tile. Has its own topbar (does NOT call `renderAppShell`); explicitly calls `startHeartbeat()`.
- `css/style.css` — full theme. Tokens, login, topbar, tiles, modal, app-shell, sub-nav, tables, pills, action-links (with `:disabled`), perm-groups, modal-hint, form groups, responsive.
- `js/app.js` — `getToken`, `getUser`, `logout`, `apiFetch` (401 → clear + redirect), `requireAuth`, `startHeartbeat` (60s pings to `/auth/me`, idempotent), `formatLastSeen` (Today/Yesterday/date), `confirmDanger` (themed delete modal), `renderAppShell`.

### 6.2 AuthService (port 9001) — DONE

**Java:**
- `config/SecurityConfig.java` — permits `/health`, `/auth/login`, `/auth/register`, `/auth/health` (legacy), `/actuator/**`, `/css/**`, `/js/**`, `/ui/**`, `/login.html`. Wires `JwtAuthFilter`. Stateless. CSRF disabled.
- `filter/JwtAuthFilter.java` — same shape as gateway's, different package. Validates via `parseToken` (throws on signature/expiry failure), sets SecurityContext.
- `util/JwtUtil.java` — `generateToken` (used at login), `parseToken`, `getUserId`, `getEmail`, `getRoles`, `getPermissions`, `getWarehouses`.
- `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`. Catches `MethodArgumentNotValidException` → 400 with `{message: <first field>, fields: {field: msg}}`. Catches `HttpMessageNotReadableException` → 400 with malformed-body message.
- `controller/AuthController.java`:
  - `POST /auth/login`, `POST /auth/register`
  - `GET /auth/me` — current user info, also bumps `lastSeenAt`
  - User CRUD: `GET/POST/PUT/DELETE /auth/users`, `/auth/users/{id}`. DELETE has self-delete guard (returns 403 if `id == currentUserId`).
  - Role CRUD: `GET/POST/PUT/DELETE /auth/roles`, `/auth/roles/{id}`.
  - Permission CRUD: `GET/POST/PUT/DELETE /auth/permissions`, `/auth/permissions/{id}`.
  - User-role assignment: `GET /auth/users/{userId}/roles`, `POST /auth/users/{userId}/roles/{roleId}`, `DELETE same`.
  - Role-permission assignment: `GET /auth/roles/{roleId}/permissions`, `POST /auth/roles/{roleId}/permissions/{permissionId}`, `DELETE same`.
  - Warehouse access: `POST /auth/users/{userId}/warehouses/{warehouseId}`, `DELETE same`.
- `controller/HealthController.java` — `GET /health` → `{"status":"UP","service":"AuthService"}`.
- `service/AuthService.java` — all the business logic. Notable:
  - `login()` unified message "Wrong email or password" for both email-not-found and password-wrong (email enumeration safe). Distinct "Account is disabled" for deactivated users.
  - `mapUserToDto()` computes `online` as `lastSeenAt > now - 90 seconds` (relies on 60s frontend heartbeat).
  - `deleteRole()` and `deletePermission()` cascade-delete join rows before the parent, so FK violations don't happen.
  - All `@Transactional` methods are annotated explicitly.
- `model/` — `AppUser`, `Role`, `Permission`, `UserRole`, `RolePermission`, `WarehouseAccess`. All map to `auth` schema. No lazy `@OneToMany` collections inside DTOs.
- `repository/` — JPA interfaces with derived query methods plus a few native queries (`findRoleIdsByUserId`, `findPermissionCodesByRoleIds`, `findByRoleId`).
- `dto/` — `LoginRequest` (email+password with `@Valid` constraints), `LoginResponse` (`{token, user: {id, email}}`), `RegisterRequest`.

**Static (dual-mode mirror — byte-identical to gateway's `app.js` + `style.css`):**
- `login.html` — mirror of gateway's, but redirects to `/ui/auth-service/index.html` after login (gateway redirects to `/menu.html`).
- `js/app.js` — identical mirror.
- `css/style.css` — identical mirror.
- `ui/auth-service/index.html` — Overview landing with 4 tile cards: Users (ready), Roles (ready), Permissions (ready), Warehouse Access (SOON).
- `ui/auth-service/users.html` — full CRUD + role assignment sub-modal + themed danger-delete + disabled delete for current user. Shows green/red dot + "Just now" or `Today/Yesterday/date`.
- `ui/auth-service/roles.html` — full CRUD + permission assignment sub-modal (grouped by namespace, with re-login warning hint).
- `ui/auth-service/permissions.html` — full CRUD.

**Database — `auth` schema** is the only schema currently seeded:
- `app_user` — id, email (unique), password_hash, active, created_at, last_seen_at
- `role` — id, name (unique)
- `permission` — id, code (unique)
- `user_role` — composite join (user_id, role_id)
- `role_permission` — composite join (role_id, permission_id)
- `warehouse_access` — composite join (user_id, warehouse_id)
- **Seed:** at least one user (`Deus@acme.com` or `sam@acme.com`), one role (e.g. `BUYER`), permissions per the PDF (`po.create`, `po.approve`, `po.submit`, `stock.read`), warehouse access [1, 3].

---

## 7. What's Next — Tier 1 (Master Data)

Build in any order; they don't depend on each other. The **template for every Tier 1+ service** is below.

### 7.1 Catalog Service (port 9002)
**Entities (own `catalog` schema):**
- `category` — hierarchical (parent_id self-FK)
- `product` — code, name, brand, status, dimensions, category_id (FK)
- `sku` — variant of a product (size, color, etc.), product_id (FK), uom_id (FK to `uom.uom` cross-schema)
- `product_attribute` — flexible key-value attributes per product

**Endpoints:** CRUD on each, plus `/products/search?keyword=`, `/products/category/{id}`, `/products/status/{status}`, `/skus/by-product/{productId}`.

### 7.2 UoM Service (port 9013)
**Entities (`uom` schema):**
- `uom` — code (e.g. EA, KG, L), description
- `uom_conversion` — from_uom_id, to_uom_id, factor

**Endpoints:** CRUD on `uom`, conversion lookup `/uom/{from}/to/{to}` returns the factor.

### 7.3 Warehouse Service (port 9004)
**Entities (`warehouse` schema):**
- `warehouse` — id, code, name, address
- `zone` — warehouse_id, code, type (RECEIVE, STORE, SHIP)
- `bin` — zone_id, code, position

**Endpoints:** CRUD on each. `/warehouses/{id}/zones`, `/zones/{id}/bins`.

### 7.4 Supplier Service (port 9005)
**Entities (`supplier` schema):**
- `supplier` — code, name, status, payment terms
- `supplier_contact` — supplier_id, name, email, phone, role
- `supplier_pricelist` — supplier_id, sku_id (cross-schema FK to `catalog.sku`), unit_price, currency, valid_from, valid_to

**Endpoints:** CRUD plus `/suppliers/{id}/contacts`, `/suppliers/{id}/pricelists`.

---

## 8. Template — Adding a New Tier 1+ Service

For each new service, repeat exactly (this works because of the rigid contract above):

1. **Module skeleton:** `pom.xml`, `mvnw`, `mvnw.cmd`, src layout, package `com.inventory.{name}`. Dependencies match `AuthService/pom.xml`: spring-boot-starter-web, spring-boot-starter-security, spring-boot-starter-data-jpa, postgresql, jjwt-api/impl/jackson, validation.
2. **`application.properties`:**
   ```properties
   spring.application.name={ServiceName}
   server.port=900X
   spring.datasource.url=jdbc:postgresql://localhost:5432/inventory_deus
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.hibernate.ddl-auto=validate
   security.jwt.secret=${INVENTORY_JWT_SECRET}
   security.jwt.expiry-minutes=60
   server.servlet.encoding.charset=UTF-8
   server.servlet.encoding.enabled=true
   server.servlet.encoding.force=true
   ```
3. **`util/JwtUtil.java`** — copy from `AuthService/util/JwtUtil.java`, change only the package. Drop `generateToken` (only auth-service issues tokens).
4. **`filter/JwtAuthFilter.java`** — copy from `AuthService/filter/JwtAuthFilter.java`, change only the package.
5. **`config/SecurityConfig.java`** — copy from `AuthService/config/SecurityConfig.java`. Update permit-list to include `/health`, `/css/**`, `/js/**`, `/ui/**`, `/login.html`, **`/auth/login`, `/auth/register`** (last two are for direct-mode login proxy). Wire `JwtAuthFilter` via `.addFilterBefore(...)`.
6. **`exception/GlobalExceptionHandler.java`** — copy from `AuthService/exception/GlobalExceptionHandler.java`. Same package change.
7. **`controller/HealthController.java`** — copy and change the `service` name in the return Map.
8. **`controller/AuthProxyController.java`** — copy from `CatalogService/controller/AuthProxyController.java`. Has **three** mappings: explicit `@PostMapping("/auth/login")` + `@PostMapping("/auth/register")` (public, no auth header forwarded), and a catch-all `@RequestMapping("/auth/**", method = {GET,POST,PUT,DELETE})` that forwards everything else WITH the Authorization header (needed for `/auth/me` heartbeat, `/auth/logout`, and any future authenticated `/auth/*` endpoints). All three go through a private `forward(...)` helper using a timeout-configured `RestTemplate` (1s connect, 2s read) that catches `HttpClientErrorException | HttpServerErrorException` (forwards downstream status + body) and `ResourceAccessException` (503 with "Auth service is not reachable" message). Without this controller, direct-mode at `localhost:<port>/` returns 403 on login and silently 404s the heartbeat — making single-session enforcement and online tracking useless on this service.
9. **`application.properties`** — also add `services.auth.url=http://localhost:9001` (the AuthProxyController reads it).
10. **Update gateway:**
   - Add `services.{name}.url=http://localhost:900X` to `ApiGateway/application.properties`.
   - Inject in `RouteController` constructor.
   - Add to the `urlsMap`.
   - Add a tile to `menu.html` services array with `healthPath: '/health/{name}'` and `uiPath: '/ui/{name}/index.html'`.
11. **Database schema:** create `CREATE SCHEMA {name};` and run table DDL. `auth` already exists. Use cross-schema FKs where the connections PDF specifies.
12. **Static folder (dual-mode mirror):** copy `AuthService/src/main/resources/static/{login.html, css/style.css, js/app.js}` verbatim — keep byte-identical. Update only the post-login redirect in `login.html` to `/ui/{name}/index.html`. **Do not write "stub" versions** — see burn marks below.
13. **UI pages:** `static/ui/{name}/index.html` with tile-grid landing, plus per-entity pages following the `users.html` / `roles.html` / `permissions.html` pattern. All call `renderAppShell({ service, page, tabs })`. All delete actions use `confirmDanger`.
14. **Run** `mvn spring-boot:run` on this service. Verify: tile lights green on `/menu.html`, click opens its UI, JWT trickles through, CRUD works. Also verify **direct-mode**: `localhost:<port>/login.html` accepts credentials and lands on `/ui/{name}/index.html` with the full theme.

---

## 9. Backlog / Parking Lot

- **`@PreAuthorize` enforcement across all mutating endpoints.** Per PDF Authentication §5.1–5.2, every controller method that writes data should be gated by a permission claim — e.g. `@PreAuthorize("hasAuthority('product.create')")` on `ProductController.create`. Currently the data plane is built (roles, permissions, role-permission assignment all CRUD; permissions ride in JWT claims; downstream `JwtAuthFilter`s convert them to Spring `SimpleGrantedAuthority` objects) but **no controller enforces them yet** — any authenticated user can do anything. Pick this up when the first procurement endpoint demos a real permission check (e.g. `po.approve`); wire the annotation pattern there and propagate backward to catalog, auth-service, and every other tier as it lands. Adding annotations now would gate everything behind permissions nobody has yet, breaking testing.
- **User activity tracker ("Track" button per user row).** A button in the Users table action column that opens a modal/page showing every meaningful action a user has taken (logins, CRUD operations, role changes, etc.). Requires: new `user_activity` table (id, user_id, action_type, resource_type, resource_id, metadata jsonb, created_at), service-side write-aside on every authenticated mutation, a query endpoint, and a themed timeline UI. Scope ≈ 2-3 hours of work; touches every service eventually (each one writes its own activity rows). Park until master-data tier is done.
- **Rate-limit `/auth/login`** — PDF Part 8 hard rule. Not implemented yet. Cheapest path: Bucket4j filter on auth-service.
- **Refresh tokens** — out of scope per PDF Part 7.3. Re-login on expiry is acceptable for class.
- **Eureka service registry (port 9017)** — not yet built. Currently the gateway hardcodes service URLs in `application.properties`. Once Eureka is in, gateway can resolve by service name.
- **Actuator** — services currently expose `/health` as hand-rolled controllers. Could swap to `spring-boot-starter-actuator` later, but not needed.
- **Database schema bootstrapping** — currently hand-create schemas in psql. Could add Flyway/Liquibase migrations per service later.
- **CSV export on tables** — nice-to-have on data-table pages.
- **Toast notifications** — currently using `alert()` for transient errors. Could swap to in-page non-blocking toast.
- **Self-deactivate guard** — admin currently *can* set their own `active=false` (which would lock them out on next login). Worth blocking, but not urgent.
- **"Service is disabled" UX** — log out screen when an active session's user is deactivated (currently JWT keeps working until exp).
- **Delete cleanup for AuthController orphan** — the legacy `/auth/health` endpoint was removed; nothing references it. Already deleted.

---

## 10. Conventions

- **JWT claims:** `sub` (user id as string), `email`, `roles` (array of role names), `perms` (array of permission codes), `whs` (array of warehouse ids), `iat`, `exp`. See PDF Authentication §2.3.
- **Permission code format:** `dot.namespace` — e.g. `po.create`, `po.approve`, `stock.read`, `user.delete`. Group prefix = domain.
- **Role name format:** `UPPER_SNAKE` — e.g. `BUYER`, `WAREHOUSE_OPS`, `ADMIN`.
- **HTTP status discipline:** 200 success, 201 created, 400 validation, 401 unauthenticated (client bounces), 403 forbidden (client shows error), 404 not found, 409 conflict (e.g. duplicate code), 503 downstream unreachable.
- **Response shape on error:** always `{"message": "<human readable>"}`. Optional `fields` object for field-level validation.
- **sessionStorage keys:** `inv_jwt`, `inv_user`. User object = `{id, email}`.
- **UI tile status:** `up` (green glow), `down` (red), `unknown`/default (grey).
- **Heartbeat:** 60s interval, `/auth/me`. Online window: 90s.
- **Service ID in URLs:** kebab-case (`auth-service`, but the API prefix is `/auth`). For Tier 1+, service IDs are single words (`catalog`, `uom`, `warehouse`, `supplier`).

---

## 11. Reference Documents

In `InventoryProper/`:
- `Inventory-Deus-Authentication.pdf` — JWT model, login flow, per-service auth chain, hard rules. **Read this if touching anything security-related.**
- `Inventory-Deus-Entities.pdf` — what each entity represents per service.
- `Inventory-Deus-Table-Connections.pdf` — every PK/FK across schemas. **Read this when designing any new table.**
- `read heirarchy.pdf` — Tier 0 → 4 build order. Do not skip tiers.
- `project-context.txt` — terse summary of the same; useful when context budget is tight.

---

## 12. Mistakes Prior AIs Have Made (avoid)

- Deleted the dual-mode mirror copies of `static/js/app.js` and `static/css/style.css` thinking they were dead code → broke direct-access mode.
- Wrote "stub" versions of `static/js/app.js`, `static/css/style.css`, `static/login.html` instead of full byte-identical mirrors of the gateway's. Same effect as deletion — `renderAppShell`, `confirmDanger`, `startHeartbeat`, the entire dark theme, the eye toggle, etc. were all missing from direct-mode access. Heuristic: if a service's static file is dramatically smaller than the gateway's, it's wrong. Always copy verbatim, edit only the one allowed line (the post-login redirect).
- Built a service's per-port `login.html` and forgot that its `fetch('/auth/login', ...)` resolves to its own origin, which has no such endpoint. Direct-mode login returns 403. Fix: every service needs an `AuthProxyController` that forwards `/auth/login` and `/auth/register` to auth-service. Mirrors the gateway's `RouteController.forwardLogin` / `forwardRegister`.
- Wrote pages with CSS classes like `.container`, `.service-content`, `.service-toolbar` that don't exist in the theme → page rendered as unstyled browser defaults.
- Built modals with `style="display:none"` toggled by `style.display='block'` → wrong; we use `.modal-back` wrapper + `.classList.add('show')` for backdrop blur + animation.
- Added tabs pointing to pages that don't exist yet → 404 on click.
- Stripped `/ui/{service}` prefix in `RouteController` before forwarding → broke static file serving on downstream services. (Fixed; do not re-introduce.)
- Used `restTemplate.postForEntity(...)` without try/catch → on 4xx, RestTemplate throws, Spring wraps in 500-with-empty-body, frontend `res.json()` throws "Unexpected end of JSON input". (All proxy methods now catch `HttpClientErrorException | HttpServerErrorException` and `ResourceAccessException`.)
- Used `window.confirm()` for destructive actions → ugly OS dialog clashes with the theme. Use `confirmDanger(...)`.
- Used `window.alert(...)` for "failed to do X" popups → same problem, ugly OS dialog. Use `alertModal({ title, body })` (in `app.js`).
- Forwarded a downstream 4xx/5xx response body using `ResponseEntity.status(...).contentType(APPLICATION_JSON).body(e.getResponseBodyAsString())` — Spring's converter chain silently dropped the body, response went out as `Content-Length: 0`. Frontend got an empty body, fell back to "Sign-in failed (HTTP 401)" instead of "Wrong email or password". Fix: forward as `byte[]` with explicit `.contentLength(bodyBytes.length)`. Bytes go through `ByteArrayHttpMessageConverter` which writes raw, bypassing the string-converter weirdness. Applies to every proxy controller (gateway's `RouteController` and per-service `AuthProxyController`s).
- Added `@PreAuthorize("hasAuthority('X')")` on a controller method without also adding `@EnableMethodSecurity` to the SecurityConfig — Spring silently ignores the annotation, the gate doesn't fire, and the endpoint is wide open. There's no compile error, no startup warning, nothing. **Both** must be present: `@EnableMethodSecurity` on the `@Configuration` class, `@PreAuthorize` on the method. Companion: the authority being checked has to actually be on the principal's authority list — if `JwtAuthFilter` doesn't translate the JWT claim into a `SimpleGrantedAuthority`, `hasAuthority(...)` returns false regardless of the claim. We add `MANAGER` from the JWT `manager` claim; mirror this pattern in every service that needs role/permission gating.
- Forgot to add `force=true` to `server.servlet.encoding` → Spring serves HTML without charset header, browser decodes as Latin-1, `×` becomes `Ã—`, `@` becomes `å`.

---

## 13. Progress Journal

> Newest at top. One line per change with file paths. Why beats what.

### 2026-05-16 (latest++)
- **Client-side search on Users and Products tables.** Themed `.search-input` added to `style.css` in all three mirrors (gateway, auth, catalog). On `users.html` the search matches against `email` and `id` (case-insensitive substring). On `products.html` it matches against `code`, `name`, `brand`, `categoryName`, and `description`. Refactored `loadUsers` / `load` to populate an `allUsers` / `allProducts` array and call `renderUsers` / `renderProducts` separately — same render function fires on initial load, on auto-refresh (every 10s), on tab focus, and on every input keystroke. Empty-state row shows `No <thing> match "query"` when the filter eliminates everything. No backend changes — search runs entirely in the browser over already-fetched rows. Pattern is directly portable to roles.html, permissions.html, and the other catalog pages (categories/skus/attributes) if you want them later.

### 2026-05-16 (latest)
- **Catalog: auto-generated product codes + bulk product creation.** Per the Entities PDF (Catalog §1), Product is the conceptual item; SKU is the atomic sellable unit. The product `code` field is now auto-generated as `PROD-NNNN` (4-digit zero-padded, starting at 1, taking the next slot above the current max). UI: the Code field is hidden on Create (auto), shown on Edit (rename allowed). New Quantity field on Create lets the admin spin up N products at once with sequential codes — e.g., quantity=5 with max existing `PROD-0007` → creates `PROD-0008` through `PROD-0012`. Backend: `ProductService.createBulk` does the loop with `repo.saveAll`; `findMaxSerial` scans existing PROD-prefixed codes and returns the highest numeric suffix. `ProductController.create` reads `body.quantity` and routes to `createBulk` when > 1. Sanity cap at 100 per request. Same pattern can be ported to other CRUD entities (SKUs, categories) if they need bulk creation.

### 2026-05-16 (later)
- **Non-managers can now only edit their own account.** Frontend: `Edit` button now hidden on other users' rows when `isManager()` is false (own row still shows it); `Roles` button hidden on every row for non-managers (was already gated server-side, just bringing the UI in line). Rows where no action applies show a "View only" tag. Backend: `AuthController.updateUser` now does a scope check first — if `!callerIsManager && id != callerId` → 403 `You can only edit your own account`. The existing field-level check (only managers can flip the `manager` flag) still runs after, even on self-edit.

### 2026-05-16
- **Extended manager gating to user creation and deletion.** `+ NEW USER` and per-row `Delete` on `users.html` now hide entirely for non-managers (not just disabled — gone from the DOM). Backend: `@PreAuthorize("hasAuthority('MANAGER')")` added to `AuthController.register` and `AuthController.deleteUser` (the existing self-delete guard runs *after* the auth check). Direct API calls from non-managers now return 403 with the themed `You don't have permission for this action` message. Note: `Edit` and `Roles` buttons stay visible to non-managers; field-level gates (manager-flag check in `updateUser`, role-assignment `@PreAuthorize`) handle the actual privilege boundaries.

### 2026-05-15 (latest++)
- **Server-side manager enforcement.** UI gates can be bypassed with DevTools; backend gates can't. Closed the self-promotion hole.
  - `AuthService/SecurityConfig` gained `@EnableMethodSecurity` so `@PreAuthorize` actually fires (it's silently ignored without this annotation).
  - `AuthService/JwtAuthFilter` now adds `new SimpleGrantedAuthority("MANAGER")` when the JWT's `manager` claim is true, so `hasAuthority('MANAGER')` works.
  - `AuthController` mutation endpoints gated with `@PreAuthorize("hasAuthority('MANAGER')")`: createRole/updateRole/deleteRole, createPermission/updatePermission/deletePermission, assignRoleToUser/removeRoleFromUser, assignPermissionToRole/removePermissionFromRole, grantWarehouseAccess/revokeWarehouseAccess. Read endpoints (GET) stay open per the view-only design.
  - `AuthController.updateUser` uses a manual conditional check (not `@PreAuthorize`) because the gate is field-level: only the `manager` field is manager-only. Other updates (email/active/password) stay open. Spring's `Authentication` is injected as a method parameter; we inspect its authorities.
  - `GlobalExceptionHandler` now catches `AccessDeniedException` (thrown by `@PreAuthorize` failures) and returns 403 with `{"message": "You don't have permission for this action. Manager rights required."}` — so the frontend's `alertModal` shows a clean themed popup instead of an opaque 403.
  - **Gateway and catalog JwtAuthFilters not updated.** They don't have manager-gated endpoints. Future services that need manager gating should mirror auth-service's pattern: add `getManager` to JwtUtil, add the MANAGER authority in JwtAuthFilter, add `@EnableMethodSecurity` to their SecurityConfig, add the `AccessDeniedException` handler.
  - **Test:** Open Roles or Permissions as a non-manager. Use DevTools to fire `fetch('/auth/roles', { method: 'POST', headers: { Authorization: 'Bearer ' + sessionStorage.getItem('inv_jwt'), 'Content-Type': 'application/json' }, body: '{\"name\":\"HACK\"}' })`. Should now get **403** with "You don't have permission..." instead of 201 Created.

### 2026-05-15 (latest)
- **Manager flag + view-only mode for non-managers + Active toggle on Roles & Permissions.**
  - **Schema:** added `auth.app_user.manager` (BOOLEAN, default false; Deus id=1 = true via UPDATE), `auth.permission.active` (BOOLEAN, default true), `auth.role.active` (BOOLEAN, default true). Run via `ALTER TABLE`.
  - **JWT:** new `manager` claim. Token issued at login now carries it. Frontend reads it via `decodeToken()` helper in `app.js`. Helpers added: `decodeToken()`, `isManager()`, `myRoles()`, `myPerms()`. All three `app.js` mirrors (gateway, auth, catalog).
  - **Java:** `AppUser.manager`, `Permission.active`, `Role.active` fields. `JwtUtil.generateToken` now takes `boolean manager` and adds the claim. `JwtUtil.getManager(claims)` helper. `AuthService.login` passes `user.getManager()` to token generation. `AuthService.updateUser` accepts `manager` key. `createRole`/`updateRole`/`createPermission`/`updatePermission` signatures take `active`. `mapUserToDto`/`mapRoleToDto`/`mapPermissionToDto` now include their new flags. `AuthController` mutation endpoints accept `Map<String, Object>` to pass `active` through.
  - **Users page:** new `Manager` column (green "Manager" pill or em-dash). Edit modal has a `Manager` checkbox below `Active` — only visible to current managers (UI gate; the backend update silently drops `manager` from the request body if `isManager()` is false on the client, so non-managers can't promote themselves even by inspecting the modal).
  - **Roles page:** `Status` column (active/inactive dot), `Yours` column (green dot if the role is in your JWT's `roles` claim). `+ NEW ROLE` button hidden for non-managers; per-row Edit/Permissions/Delete replaced with `View only` text. `Active` checkbox in the create/edit modal. Auto-refresh wired (10s + on focus) for parity with users.html.
  - **Permissions page:** same shape — `Status`, `Yours` (your effective perms from JWT `perms`), `+ NEW PERMISSION` gated, per-row actions gated, `Active` checkbox in modal, auto-refresh.
  - **What still isn't enforced server-side:** the `manager` flag is only checked client-side (UI gate). Any authenticated user can hit `POST /auth/roles`, `DELETE /auth/users/{id}`, etc. and the backend still accepts it. Real enforcement waits for the `@PreAuthorize` backlog item (section 9). Treat current behavior as "honest user gating" — visible affordances respect the rule; the API does not.
  - **Carryover gotcha:** users with a JWT issued *before* this change have no `manager` claim. `isManager()` returns false until they log out and back in. Document this so testers don't think the UI is broken.

### 2026-05-15 (later)
- **Auto-refresh on the Users table.** Added `setInterval(loadUsers, 10000)` and `window.addEventListener('focus', loadUsers)` to `users.html` so the table reflects other users' online transitions without needing a manual refresh. Earlier the table only reloaded after a save/delete, so freshly-created users (or users whose own heartbeat stopped) appeared frozen. Polling at 10s + on-focus is the cheap UX-wins-by-most ratio.
- Added a backlog item to section 9: wire `@PreAuthorize("hasAuthority('...')")` across mutating controllers when the first procurement endpoint lands. Today's roles/permissions data plane is built but nothing enforces them — pure theatre until the annotations show up. Doing it now would break unauthenticated catalog testing, so it waits for a real journey.

### 2026-05-15
- **Tightened presence window: heartbeat 60s → 1s, online threshold 90s → 3s.** Edited `AuthService.mapUserToDto` and `AuthService.login` (single-session check) for the threshold; edited the `startHeartbeat` default in all three `app.js` mirrors. Trade-off: ~60 pings/min per active user (was 1/min). Fine for a class demo, would need rethinking for production. Result: online dot flips within ~3s of a tab closing, and single-session lockout window after closing a tab without logout is ~3s instead of 90s.
- **Fixed silent body loss in proxy error responses.** Gateway's `forward()` / `forwardAuthApi` / `forwardToService` (and catalog's `AuthProxyController.forward`) were returning `ResponseEntity<?>` with a `String` body + `Content-Type: application/json` in their 4xx/5xx catch blocks. Spring wrote the response with `Content-Length: 0` and empty body, so the frontend saw `Sign-in failed (HTTP 401)` instead of the actual `Wrong email or password` or `Already signed in from another tab or device`. Verified the bug with `curl -i` directly against the gateway. Fix: use `byte[] bodyBytes = e.getResponseBodyAsByteArray()` and `.contentLength(bodyBytes.length).body(bodyBytes)` in every catch block. Also changed `AuthProxyController` method signatures from `ResponseEntity<String>` to `ResponseEntity<?>` so `byte[]` body type-checks.

### 2026-05-14 (latest)
- **Themed popups everywhere.** Added `alertModal({ title, body, tag })` + `closeAlert()` to all three `app.js` mirrors (gateway, auth, catalog) — single-button informational modal matching `confirmDanger` / "Service not running" style. Body is optional; auto-hides if not provided. Wired Esc + click-outside + X + CLOSE to dismiss.
- Replaced **40** `window.alert(...)` calls across 7 app pages (users, roles, permissions, products, categories, skus, attributes) with `alertModal({...})`. Mapping: plain strings → title-only; `alert(err.message || 'fallback')` → `alertModal({ title: 'fallback', body: err.message })`.
- Replaced inline `.error hidden` div pattern in all 3 `login.html` catch blocks with `alertModal({ title: 'Sign-in failed', body: e.message })`. The dead `.error` div is left in the HTML for now (harmless, hidden, can be cleaned up later).
- **Rule for new pages:** use `alertModal` for any "something failed" popup. Use `confirmDanger` for destructive confirmations. **Never** use `window.alert()` or `window.confirm()` — they bypass the theme.
- Fix: added missing imports `RequestMapping` and `RequestMethod` to `CatalogService/AuthProxyController.java` — compile error after introducing the catch-all `@RequestMapping("/auth/**", method = {RequestMethod.GET, ...})`. Always import both when adding a catch-all to a controller that previously only had `@PostMapping`s.
- **Single-session enforcement.** Added a threshold check in `AuthService.login()` — if `lastSeenAt > now - 90s`, throws `"Already signed in from another tab or device..."`. Heartbeat keeps `lastSeenAt` fresh on the active tab, so a second tab/device hitting login lands inside the window and is rejected. After a clean logout, `lastSeenAt` is cleared so re-login is instant.
- Added `AuthService.endSession(userId)` — clears `lastSeenAt`. Wired to a new `POST /auth/logout` endpoint in `AuthController` that reads the authenticated principal.
- Updated `logout()` in all three `app.js` copies (gateway, auth-service, catalog) to call `POST /auth/logout` (with bearer token, best-effort) before clearing sessionStorage and redirecting. Failures are ignored — client logout proceeds regardless.
- **Widened CatalogService/AuthProxyController** to a catch-all `@RequestMapping("/auth/**", method = {GET,POST,PUT,DELETE})` so `/auth/me` (heartbeat) and the new `/auth/logout` work in direct mode. The specific `@PostMapping("/auth/login")` and `/auth/register` still win the routing race for their exact paths; everything else falls through to the catch-all (which forwards the Authorization header). Without this, direct-mode catalog tabs had a silently failing heartbeat — meaning the single-session check would have been useless for catalog sessions.
- **Parked "Track" button / user activity log** to section 9 backlog. Scope is non-trivial and crosses every service; pick it up after Tier 1 master data is done.

### 2026-05-14 (later)
- **Tier 1 — CatalogService scaffolded** (port 9002, schema `catalog`). 4 entities (`Category` with self-FK parent, `Product` with `category_id` FK, `Sku` with `product_id` FK + cross-schema `uom_id` as plain Long, `ProductAttribute`). Repos + services + controllers + DTOs. `JwtAuthFilter`, `SecurityConfig`, `JwtUtil`, `GlobalExceptionHandler`, `HealthController` all copied from auth-service template.
- Added `CatalogService/controller/AuthProxyController.java` — proxies `POST /auth/login` and `POST /auth/register` to auth-service via timeout-configured RestTemplate. Required because the catalog's own `login.html` posts to a relative `/auth/login`, which would 403 without this. Catches `HttpClientError/ServerError` → forwards body intact; catches `ResourceAccessException` → 503 with friendly message.
- Added `services.auth.url=http://localhost:9001` to `CatalogService/application.properties`. Also set `spring.jpa.properties.hibernate.default_schema=catalog`.
- Updated `CatalogService/SecurityConfig` permit-list to include `/auth/login`, `/auth/register` (in addition to the standard `/health`, `/css/**`, `/js/**`, `/ui/**`, `/login.html`).
- Replaced `CatalogService/static/{js/app.js, css/style.css, login.html}` stubs with **byte-identical mirrors** of the gateway's copies. Only allowed change: `login.html`'s post-login redirect set to `/ui/catalog/index.html`. Restores the dual-mode contract for direct access at `localhost:9002`.
- Updated `menu.html`: catalog tile now has `uiPath: '/ui/catalog/index.html'` and pings `/health/catalog`.
- **Workflow change:** deepseek (web) + lingma (IDE) own structure/scaffolding for Tier 1+ services. Claude (this AI) owns cross-cutting upgrades and troubleshooting. Handoff signal: call Claude when something breaks, when a pattern needs to graduate to shared assets, or for architecture decisions.
- Updated PROGRESS.md template (section 8) to include `AuthProxyController` step and direct-mode verification. Added the stub-instead-of-mirror burn mark and the missing-auth-proxy burn mark to section 12.

### 2026-05-14
- Added `PROGRESS.md` (this file) — single onboarding/progress doc for any AI joining the project.
- Added themed danger-confirm modal: `confirmDanger()` in both `app.js` copies; swapped `window.confirm()` calls in `users.html`, `roles.html`, `permissions.html`. Why: native browser confirm clashes with dark theme.
- Added self-delete guard in `AuthController.deleteUser` — returns 403 if `id == currentUserId`. Frontend disables Delete button on own row. Why: admin must not be able to delete themselves, breaks FK history.
- Added `.action-link:disabled` styling in both `style.css` copies.
- Replaced `OFFLINE`/`ONLINE` text pills in `users.html` with green/red dot indicators. Online users now show `Just now` for Last Seen.
- Tightened online threshold from 5 min to 90 sec in `AuthService.mapUserToDto`. Why: must match the new 60s heartbeat or stale users appear online.
- Added `startHeartbeat()` to `app.js` (both copies). Pings `/auth/me` every 60s. Auto-called by `renderAppShell` and explicitly by `menu.html`. Why: keeps `lastSeenAt` fresh so the Online column actually reflects reality.
- Added defensive JSON parsing to both `login.html` files — `let json = {}; try { json = await res.json(); } catch {}`. Why: prevents cryptic "Unexpected end of JSON input" when server returns empty body.
- Removed `← ` arrow prefix from the MENU back-button — just reads `MENU` now in `renderAppShell`.
- Added robust error forwarding in `RouteController` — all 3 proxy methods (`forward`, `forwardAuthApi`, `forwardToService`) catch `HttpClientErrorException | HttpServerErrorException` → forward status + body; catch `ResourceAccessException` → 503. Why: prior behavior swallowed 4xx and returned 500-with-empty-body.
- Added 1s connect / 2s read timeouts to gateway's `RestTemplate` via `SimpleClientHttpRequestFactory`. Why: a hung downstream was freezing the menu's 2s health pings.
- Created `AuthService/controller/HealthController.java` (`/health` at root). Updated `menu.html` so the auth tile uses `/health/auth-service` like every other service.
- Removed `/auth/health` from gateway's `SecurityConfig` permit-list — no longer needed.
- Gateway `RouteController.forwardHealth` (added by IDE AI): generic `/health/{serviceName}` proxy with downstream timeout handling.
- Added namespace-grouped permission assignment modal in `roles.html` ("Permissions" button per row → opens modal grouped by `po.*`, `stock.*`, etc. with re-login warning hint).
- Added `getPermissionsForRole` service method + `GET /auth/roles/{roleId}/permissions` endpoint.
- Added role-assignment modal in `users.html` ("Roles" button per row → checkbox list of all roles). Backed by new `getRolesForUser` and `GET /auth/users/{userId}/roles`.
- Added Role CRUD and Permission CRUD endpoints + service methods. Cascading deletes: deleting a role cleans `user_role` and `role_permission`; deleting a permission cleans `role_permission`.
- Created `AuthService/exception/GlobalExceptionHandler.java` — catches `@Valid` failures, returns `{message, fields}` JSON.
- Reverted login to unified "Wrong email or password" (email-enumeration safe). Kept distinct "Account is disabled".
- Added `roles.html`, `permissions.html`, restructured `index.html` as proper landing with tile-grid.
- Themed the entire project (full dark/techy palette in `style.css`, Syne + Space Mono fonts, glow effects, fadeUp/pop animations, grid background). Mirrored `app.js` and `style.css` into auth-service for dual-mode.
- Created `JwtAuthFilter.java` in auth-service. Wired into `SecurityConfig`. Fixed the 403 on `/auth/users` (root cause: no filter was validating the token downstream).
- Added `renderAppShell({service, page, tabs})` helper to `app.js`. Added `.app-shell`, `.back-link`, `.crumb`, `.sub-nav` to `style.css`.
- Fixed `RouteController.forwardToService` to forward the full URI (was stripping `/ui/{service}` and breaking static file serving on downstream).
- Added `formatLastSeen()` helper — Today/Yesterday/date logic.
- Forced UTF-8 in both services' `application.properties` to kill mojibake.

### Earlier (Tier 0 foundation)
- Initial scaffold of ApiGateway and AuthService.
- Basic JWT filter on gateway.
- `auth` schema bootstrap with seed user/role/permissions.
- Initial `login.html`, `menu.html`, `users.html`.

---

*End of PROGRESS.md. Update the journal section at the end of every work session — one line per change, with file path and why.*
