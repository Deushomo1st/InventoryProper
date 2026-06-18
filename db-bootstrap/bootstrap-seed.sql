-- =============================================================================
-- ElectDeptSoftware — Seed Data
-- =============================================================================
-- WHEN TO RUN: ONCE, AFTER bootstrap-schemas.sql has been run AND all services
-- have been started at least once (so Hibernate has created the tables under
-- each schema).
--
-- HOW TO RUN:
--   psql -U postgres -d ElectDeptSoftware -f bootstrap-seed.sql
--
-- WHAT IT DOES:
--   - Seeds the auth schema: permissions, roles, role-permission mappings,
--     one default admin user (admin@electdept.com / password: "password" —
--     CHANGE THIS IMMEDIATELY via PUT /auth/users/{id}).
--   - Seeds the uom schema with electrical units (V, A, W, kW, kWh, etc.).
--   - Seeds the notification schema with templates for the common events.
--   - Seeds asset classes (Transformer, Breaker, Cable, Relay, Switchgear).
--
-- IDEMPOTENCY: Most inserts use ON CONFLICT DO NOTHING so re-running is safe.
-- =============================================================================

\connect ElectDeptSoftware

-- -------------------------------------------------------------------
-- 1. PERMISSIONS
-- -------------------------------------------------------------------
-- Granular permissions per service. Convention: <service>.<verb>
INSERT INTO auth.permission (code, active) VALUES
    -- AssetManagement
    ('asset.read', true), ('asset.write', true), ('asset.maintain', true),
    -- CircuitDesign
    ('circuit.read', true), ('circuit.write', true), ('circuit.approve', true),
    -- LoadCalculation
    ('loadcalc.read', true), ('loadcalc.write', true), ('loadcalc.execute', true),
    -- CableSizing
    ('cable.read', true), ('cable.write', true), ('cable.execute', true),
    -- ProtectionCoordination
    ('protection.read', true), ('protection.write', true), ('protection.execute', true),
    -- EnergyMonitoring
    ('energy.read', true), ('energy.write', true),
    -- WorkOrder
    ('workorder.read', true), ('workorder.write', true), ('workorder.approve', true), ('workorder.dispatch', true),
    -- OutageManagement
    ('outage.read', true), ('outage.write', true), ('outage.dispatch', true), ('outage.restore', true),
    -- SCADAIntegration
    ('scada.read', true), ('scada.write', true), ('scada.ingest', true),
    -- GISMapping
    ('gis.read', true), ('gis.write', true),
    -- BillingTariff
    ('billing.read', true), ('billing.write', true), ('billing.approve', true),
    -- RegulatoryReporting
    ('regulatory.read', true), ('regulatory.write', true), ('regulatory.submit', true),
    -- DocumentManagement
    ('document.read', true), ('document.write', true), ('document.approve', true),
    -- Auth & Permissions
    ('auth.read', true), ('auth.write', true),
    -- Audit
    ('audit.read', true),
    -- Notification
    ('notification.read', true), ('notification.write', true),
    -- UOM
    ('uom.read', true), ('uom.write', true)
ON CONFLICT (code) DO NOTHING;

-- -------------------------------------------------------------------
-- 2. ROLES
-- -------------------------------------------------------------------
INSERT INTO auth.role (name, active) VALUES
    ('ADMIN',              true),  -- full access
    ('SENIOR_ENGINEER',    true),  -- design/calc authority
    ('FIELD_TECHNICIAN',   true),  -- work orders + reading meters
    ('LINE_CREW',          true),  -- outage dispatch & restore
    ('TARIFF_MANAGER',     true),  -- billing + rate adjustments
    ('COMPLIANCE_OFFICER', true),  -- regulatory + audit
    ('CONTRACTOR',         true)   -- read-only external party
ON CONFLICT (name) DO NOTHING;

-- -------------------------------------------------------------------
-- 3. ROLE-PERMISSION MAPPINGS
-- -------------------------------------------------------------------
-- ADMIN: every permission
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'ADMIN'), p.id FROM auth.permission p
ON CONFLICT DO NOTHING;

-- SENIOR_ENGINEER: design + calculation perms + asset read/write
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'SENIOR_ENGINEER'), p.id FROM auth.permission p
WHERE p.code IN (
    'asset.read','asset.write',
    'circuit.read','circuit.write','circuit.approve',
    'loadcalc.read','loadcalc.write','loadcalc.execute',
    'cable.read','cable.write','cable.execute',
    'protection.read','protection.write','protection.execute',
    'document.read','document.write',
    'gis.read','gis.write',
    'uom.read','energy.read','workorder.read','workorder.approve'
)
ON CONFLICT DO NOTHING;

-- FIELD_TECHNICIAN: work orders + meter reads
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'FIELD_TECHNICIAN'), p.id FROM auth.permission p
WHERE p.code IN (
    'asset.read','asset.maintain',
    'workorder.read','workorder.write',
    'energy.read','energy.write',
    'gis.read','scada.read',
    'document.read','uom.read'
)
ON CONFLICT DO NOTHING;

-- LINE_CREW: outage dispatch + restore
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'LINE_CREW'), p.id FROM auth.permission p
WHERE p.code IN (
    'asset.read',
    'outage.read','outage.write','outage.dispatch','outage.restore',
    'workorder.read','workorder.dispatch',
    'gis.read','scada.read'
)
ON CONFLICT DO NOTHING;

-- TARIFF_MANAGER: billing
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'TARIFF_MANAGER'), p.id FROM auth.permission p
WHERE p.code IN (
    'billing.read','billing.write','billing.approve',
    'energy.read','uom.read'
)
ON CONFLICT DO NOTHING;

-- COMPLIANCE_OFFICER: regulatory + audit
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'COMPLIANCE_OFFICER'), p.id FROM auth.permission p
WHERE p.code IN (
    'regulatory.read','regulatory.write','regulatory.submit',
    'audit.read','document.read','document.approve'
)
ON CONFLICT DO NOTHING;

-- CONTRACTOR: read-only on most things
INSERT INTO auth.role_permission (role_id, permission_id)
SELECT (SELECT id FROM auth.role WHERE name = 'CONTRACTOR'), p.id FROM auth.permission p
WHERE p.code LIKE '%.read'
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------
-- 4. DEFAULT ADMIN USER
-- -------------------------------------------------------------------
-- email: admin@electdept.com
-- password: "password" (BCrypt cost 10 — the well-known Spring Security demo hash)
-- WARNING: change immediately in production via PUT /auth/users/{id}!
INSERT INTO auth.app_user (email, password_hash, active, created_at, manager) VALUES
    ('admin@electdept.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     true, NOW(), true)
ON CONFLICT (email) DO NOTHING;

-- Grant ADMIN role to the default admin user
INSERT INTO auth.user_role (user_id, role_id, granted_at)
SELECT
    (SELECT id FROM auth.app_user WHERE email = 'admin@electdept.com'),
    (SELECT id FROM auth.role WHERE name = 'ADMIN'),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM auth.user_role
    WHERE user_id = (SELECT id FROM auth.app_user WHERE email = 'admin@electdept.com')
      AND role_id = (SELECT id FROM auth.role WHERE name = 'ADMIN')
);

-- -------------------------------------------------------------------
-- 5. UNITS OF MEASURE (electrical)
-- -------------------------------------------------------------------
INSERT INTO uom.unit (code, name, active, created_at) VALUES
    -- Base electrical
    ('V',     'Volt',                        true, NOW()),
    ('A',     'Ampere',                      true, NOW()),
    ('W',     'Watt',                        true, NOW()),
    ('VA',    'Volt-Ampere',                 true, NOW()),
    ('var',   'Volt-Ampere Reactive',        true, NOW()),
    ('Ohm',   'Ohm (resistance)',            true, NOW()),
    ('Hz',    'Hertz',                       true, NOW()),
    ('F',     'Farad (capacitance)',         true, NOW()),
    ('H',     'Henry (inductance)',          true, NOW()),
    -- Kilo / Mega multiples (common in utility scale)
    ('kV',    'Kilovolt',                    true, NOW()),
    ('kA',    'Kiloampere',                  true, NOW()),
    ('kW',    'Kilowatt',                    true, NOW()),
    ('kVA',   'Kilovolt-Ampere',             true, NOW()),
    ('kVAR',  'Kilovolt-Ampere Reactive',    true, NOW()),
    ('kWh',   'Kilowatt-Hour',               true, NOW()),
    ('MW',    'Megawatt',                    true, NOW()),
    ('MVA',   'Megavolt-Ampere',             true, NOW()),
    ('MVAR',  'Megavolt-Ampere Reactive',    true, NOW()),
    ('MWh',   'Megawatt-Hour',               true, NOW()),
    ('GWh',   'Gigawatt-Hour',               true, NOW()),
    -- Misc
    ('PF',    'Power Factor (cos phi)',      true, NOW()),
    ('m',     'Meter (cable length)',        true, NOW()),
    ('km',    'Kilometer',                   true, NOW()),
    ('mm2',   'Square millimeter (cross-section)', true, NOW()),
    ('AWG',   'American Wire Gauge',         true, NOW()),
    ('degC',  'Degree Celsius',              true, NOW())
ON CONFLICT (code) DO NOTHING;

-- -------------------------------------------------------------------
-- 6. NOTIFICATION TEMPLATES
-- -------------------------------------------------------------------
INSERT INTO notification.notification_template
    (code, subject_template, body_template, channel, active, created_at) VALUES
    ('OUTAGE_DETECTED',
     'Outage detected at ${substationName}',
     'A fault was detected on feeder ${feederName} at ${detectedAt}. Estimated ${affectedCustomers} customers affected. Crew dispatched.',
     'IN_APP', true, NOW()),
    ('OUTAGE_RESTORED',
     'Power restored: ${substationName}',
     'Outage on feeder ${feederName} restored at ${restoredAt}. Total downtime: ${downtimeMinutes} minutes.',
     'IN_APP', true, NOW()),
    ('WORK_ORDER_ASSIGNED',
     'Work order WO-${workOrderId} assigned',
     'You have been assigned ${taskName} at ${location}. Priority: ${priority}. Scheduled: ${scheduledAt}.',
     'IN_APP', true, NOW()),
    ('WORK_ORDER_OVERDUE',
     'OVERDUE: WO-${workOrderId}',
     'Work order ${workOrderId} (${taskName}) is overdue by ${hoursOverdue} hours. Please update status.',
     'IN_APP', true, NOW()),
    ('LOAD_THRESHOLD_EXCEEDED',
     'Load threshold exceeded on ${assetTag}',
     'Asset ${assetTag} (${assetClass}) exceeded ${threshold} ${unit} at ${exceededAt}. Current reading: ${currentValue}.',
     'IN_APP', true, NOW()),
    ('BILL_GENERATED',
     'Bill ready: ${billingPeriod}',
     'Your electricity bill for ${billingPeriod} is ready. Total: ${totalAmount} ${currency}. Due: ${dueDate}.',
     'IN_APP', true, NOW()),
    ('REGULATORY_FILING_DUE',
     'Compliance filing due: ${reportName}',
     '${reportName} (${standard}) is due on ${dueDate}. Last submitted: ${lastSubmittedAt}.',
     'IN_APP', true, NOW()),
    ('DOCUMENT_APPROVED',
     'Document approved: ${documentCode}',
     '${documentTitle} (rev ${revisionNumber}) was approved by ${approverName} on ${approvedAt}.',
     'IN_APP', true, NOW())
ON CONFLICT (code) DO NOTHING;

-- -------------------------------------------------------------------
-- 7. ASSET CLASSES (electrical asset taxonomy)
-- -------------------------------------------------------------------
-- The asset_class table is created by Hibernate from the AssetClass entity.
-- If the entity has a 'name' (unique) column, the inserts below populate it.
INSERT INTO asset.asset_class (name, active, created_at) VALUES
    ('Transformer',  true, NOW()),
    ('Switchgear',   true, NOW()),
    ('Breaker',      true, NOW()),
    ('Recloser',     true, NOW()),
    ('Relay',        true, NOW()),
    ('Cable',        true, NOW()),
    ('Pole',         true, NOW()),
    ('Insulator',    true, NOW()),
    ('Capacitor',    true, NOW()),
    ('Meter',        true, NOW()),
    ('Generator',    true, NOW()),
    ('Inverter',     true, NOW())
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================
SELECT 'Permissions seeded:' AS metric, COUNT(*) FROM auth.permission;
SELECT 'Roles seeded:'        AS metric, COUNT(*) FROM auth.role;
SELECT 'Role-Permission maps:' AS metric, COUNT(*) FROM auth.role_permission;
SELECT 'Users:'               AS metric, COUNT(*) FROM auth.app_user;
SELECT 'Units of measure:'    AS metric, COUNT(*) FROM uom.unit;
SELECT 'Notification templates:' AS metric, COUNT(*) FROM notification.notification_template;
SELECT 'Asset classes:'       AS metric, COUNT(*) FROM asset.asset_class;
