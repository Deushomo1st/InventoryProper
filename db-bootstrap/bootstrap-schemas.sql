-- =============================================================================
-- ElectDeptSoftware — Database & Schema Bootstrap
-- =============================================================================
-- WHEN TO RUN: ONCE, on a fresh PostgreSQL install, before starting any service.
--
-- HOW TO RUN (from psql or pgAdmin):
--   1. Connect to the default 'postgres' database as a superuser.
--   2. Execute this entire file.
--   3. Then start all services. Hibernate (ddl-auto=update) will create the
--      tables under each schema automatically.
--   4. Once all services are up, run bootstrap-seed.sql.
--
-- WHAT IT DOES:
--   - Creates the `ElectDeptSoftware` database (skips if it already exists).
--   - Creates one schema per service (18 total).
--   - Grants the 'postgres' user full permissions on each schema.
-- =============================================================================

-- Create database if missing. Postgres can't do CREATE DATABASE IF NOT EXISTS
-- in plain SQL, but a DO block via dblink-style trick won't work either —
-- so we wrap in a check via pg_database.
SELECT 'CREATE DATABASE "ElectDeptSoftware"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ElectDeptSoftware')
\gexec

-- Switch to the new database
\connect ElectDeptSoftware

-- Create the 18 schemas. Each maps to a service (see SETUP.md).
CREATE SCHEMA IF NOT EXISTS auth         AUTHORIZATION postgres;  -- AuthPermissionsService
CREATE SCHEMA IF NOT EXISTS asset        AUTHORIZATION postgres;  -- AssetManagementService
CREATE SCHEMA IF NOT EXISTS uom          AUTHORIZATION postgres;  -- UomService
CREATE SCHEMA IF NOT EXISTS gis          AUTHORIZATION postgres;  -- GISMappingService
CREATE SCHEMA IF NOT EXISTS document     AUTHORIZATION postgres;  -- DocumentManagementService
CREATE SCHEMA IF NOT EXISTS energy       AUTHORIZATION postgres;  -- EnergyMonitoringService
CREATE SCHEMA IF NOT EXISTS workorder    AUTHORIZATION postgres;  -- WorkOrderService
CREATE SCHEMA IF NOT EXISTS protection   AUTHORIZATION postgres;  -- ProtectionCoordinationService
CREATE SCHEMA IF NOT EXISTS circuit      AUTHORIZATION postgres;  -- CircuitDesignService
CREATE SCHEMA IF NOT EXISTS scada        AUTHORIZATION postgres;  -- SCADAIntegrationService
CREATE SCHEMA IF NOT EXISTS outage       AUTHORIZATION postgres;  -- OutageManagementService
CREATE SCHEMA IF NOT EXISTS loadcalc     AUTHORIZATION postgres;  -- LoadCalculationService
CREATE SCHEMA IF NOT EXISTS cable        AUTHORIZATION postgres;  -- CableSizingService
CREATE SCHEMA IF NOT EXISTS audit        AUTHORIZATION postgres;  -- AuditService
CREATE SCHEMA IF NOT EXISTS notification AUTHORIZATION postgres;  -- NotificationService
CREATE SCHEMA IF NOT EXISTS regulatory   AUTHORIZATION postgres;  -- RegulatoryReportingService
CREATE SCHEMA IF NOT EXISTS billing      AUTHORIZATION postgres;  -- BillingTariffService

-- Grant the 'postgres' user full perms (the user the services connect as).
-- If you connect as a different user, change 'postgres' below to that user.
DO $$
DECLARE
    s text;
BEGIN
    FOR s IN
        SELECT unnest(ARRAY[
            'auth','asset','uom','gis','document','energy','workorder',
            'protection','circuit','scada','outage','loadcalc','cable',
            'audit','notification','regulatory','billing'
        ])
    LOOP
        EXECUTE format('GRANT ALL ON SCHEMA %I TO postgres', s);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON TABLES TO postgres', s);
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT ALL ON SEQUENCES TO postgres', s);
    END LOOP;
END $$;

-- Sanity check: list the schemas
SELECT schema_name FROM information_schema.schemata
WHERE schema_name IN (
    'auth','asset','uom','gis','document','energy','workorder',
    'protection','circuit','scada','outage','loadcalc','cable',
    'audit','notification','regulatory','billing'
)
ORDER BY schema_name;
