# db-bootstrap/

This folder holds everything needed to bring the `ElectDeptSoftware`
PostgreSQL database from "nothing" to "ready for the services" — either
**automatically** (when you boot AuthPermissionsService) or **manually**
(if you prefer to run the SQL yourself from psql / pgAdmin).

---

## What's in here

| File | Purpose |
|---|---|
| `bootstrap-schemas.sql` | Creates the `ElectDeptSoftware` database (skips if it already exists) + the 17 service schemas (`auth`, `asset`, `uom`, `gis`, `document`, `energy`, `workorder`, `protection`, `circuit`, `scada`, `outage`, `loadcalc`, `cable`, `audit`, `notification`, `regulatory`, `billing`) + grants. Uses psql meta-commands (`\gexec`, `\connect`), so it MUST be run via psql or pgAdmin's PSQL Tool — not the regular Query Tool. |
| `bootstrap-seed.sql` | Inserts roles, permissions, role-permission mappings, the default admin user (`admin@electdept.com` / password `password`), and notification templates. Pure SQL — runs anywhere. **Idempotent** thanks to `ON CONFLICT DO NOTHING` clauses, so it's safe to run multiple times. |

---

## How the automatic bootstrap works

When you start **AuthPermissionsService**, two pieces of code run:

### 1. `DatabaseBootstrap.ensurePreSpring()` — runs BEFORE Spring boots

Lives in `AuthPermissionsService/.../bootstrap/DatabaseBootstrap.java`.
Called as the very first line of `main()`, before `SpringApplication.run(...)`.

Steps:
1. **Probe Postgres** on `localhost:5432` (or `POSTGRES_HOST` / `POSTGRES_PORT` env vars).
   - If unreachable → opens a Swing dialog telling you to install PostgreSQL,
     throws, startup aborts.
2. **Check if `ElectDeptSoftware` database exists**. If not, `CREATE DATABASE`.
3. **Ensure all 17 schemas exist** (`CREATE SCHEMA IF NOT EXISTS`).

After this, Spring's DataSource can wire up cleanly because the target DB
and its schemas all exist. Hibernate then creates the tables (via
`ddl-auto=update`) as part of the normal Spring startup.

### 2. `SeedRunner` — runs AFTER Hibernate has created the tables

A `@Component` that listens for `ApplicationReadyEvent`. After Spring is
fully up and Hibernate has materialised every `@Entity` into a real table,
the seed runner:
1. Looks for `admin@electdept.com` in `auth.app_user`.
2. If not present, reads and executes `db-bootstrap/bootstrap-seed.sql`.
3. Updates the admin user's password hash with `pgcrypto` so it actually
   decodes to `"password"` (works around a hash-mismatch issue in the
   static seed value).

After this fires, you can log in immediately at
`http://localhost:9000/login.html` with `admin@electdept.com` / `password`.

---

## Configuration

If your PostgreSQL setup isn't the defaults, override via environment
variables on the AuthPermissionsService run config:

| Variable | Default | What it controls |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | DB host |
| `POSTGRES_PORT` | `5432` | DB port |
| `POSTGRES_USER` | `postgres` | superuser for DB / schema creation |
| `POSTGRES_PASSWORD` | `postgres` | password for that user |
| `ELECTDEPT_DB_NAME` | `ElectDeptSoftware` | target database name |

---

## Manual bootstrap (skip the Java layer entirely)

If you'd rather run the SQL by hand — e.g. you're on a server, or the
Swing dialog can't show — open pgAdmin's PSQL Tool (right-click your
server → PSQL Tool) and run:

```
\i /path/to/ElectDeptSoftware/db-bootstrap/bootstrap-schemas.sql
\i /path/to/ElectDeptSoftware/db-bootstrap/bootstrap-seed.sql
```

Or via psql command line:

```bash
psql -U postgres -f db-bootstrap/bootstrap-schemas.sql
psql -U postgres -d ElectDeptSoftware -f db-bootstrap/bootstrap-seed.sql
```

The automatic flow will then see "everything already exists" and proceed
straight to Spring startup with no extra work.
