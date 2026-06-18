package com.electdept.authpermissionsservice.bootstrap;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Pre-Spring database bootstrap.
 *
 * Call {@link #ensurePreSpring()} as the very first line of
 * AuthPermissionsServiceApplication.main() — BEFORE
 * {@code SpringApplication.run(...)}. The reason is timing:
 * Spring's DataSource is configured to connect to the target database
 * (ElectDeptSoftware). If that database doesn't exist yet, Spring's
 * Hikari pool fails to initialise and the whole context refresh dies
 * with no meaningful message for a beginner ("FATAL: database
 * ElectDeptSoftware does not exist"). By running this check first we
 * can:
 *
 *   1. Confirm PostgreSQL is reachable at all (and show a friendly
 *      Swing dialog with the download link if it isn't).
 *   2. Create the ElectDeptSoftware database if it's missing.
 *   3. Ensure every service schema exists (CREATE SCHEMA IF NOT EXISTS).
 *
 * After this returns, Spring is free to wire its DataSource against a
 * known-good DB and Hibernate's ddl-auto=update handles the tables.
 *
 * The seed data (admin user, roles, permissions, notification
 * templates) is loaded LATER — see {@link SeedRunner} which fires on
 * ApplicationReadyEvent, after Hibernate has built the tables.
 *
 * Connection params come from these env vars (defaults in parens):
 *   POSTGRES_HOST     (localhost)
 *   POSTGRES_PORT     (5432)
 *   POSTGRES_USER     (postgres)
 *   POSTGRES_PASSWORD (postgres)
 *   ELECTDEPT_DB_NAME (ElectDeptSoftware)
 */
public final class DatabaseBootstrap {

    private DatabaseBootstrap() {}

    private static final String HOST     = envOr("POSTGRES_HOST", "localhost");
    private static final String PORT     = envOr("POSTGRES_PORT", "5432");
    private static final String USER     = envOr("POSTGRES_USER", "postgres");
    private static final String PASSWORD = envOr("POSTGRES_PASSWORD", "postgres");
    private static final String DB_NAME  = envOr("ELECTDEPT_DB_NAME", "ElectDeptSoftware");

    /** The 17 schemas, one per service. Keep in sync with bootstrap-schemas.sql. */
    private static final List<String> SCHEMAS = List.of(
            "auth", "asset", "uom", "gis", "document", "energy", "workorder",
            "protection", "circuit", "scada", "outage", "loadcalc", "cable",
            "audit", "notification", "regulatory", "billing"
    );

    public static void ensurePreSpring() {
        log("==== ElectDept DB bootstrap ====");

        // (1) Probe Postgres. If it's down or not installed at all, we can't
        // do anything useful — show a friendly dialog and bail loudly.
        if (!isPostgresReachable()) {
            showPostgresMissingAlert();
            throw new IllegalStateException(
                    "PostgreSQL is not reachable at " + HOST + ":" + PORT +
                    ". Install it (see dialog or console message) and re-run.");
        }
        log("PostgreSQL reachable at " + HOST + ":" + PORT + " as user '" + USER + "'.");

        // (2) Ensure the target database exists. We connect to the built-in
        // 'postgres' database for this — CREATE DATABASE can't run inside
        // the database you're trying to create.
        ensureDatabaseExists();

        // (3) Ensure every service schema exists. Switch our connection to
        // the target database for this step.
        ensureSchemasExist();

        log("Bootstrap complete. Handing off to Spring.");
        log("================================");
    }

    /* ----------------------- internals ----------------------- */

    private static boolean isPostgresReachable() {
        try (Connection c = openMaster()) {
            return c.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void ensureDatabaseExists() {
        try (Connection c = openMaster()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, DB_NAME);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        log("Database '" + DB_NAME + "' already exists.");
                        return;
                    }
                }
            }
            log("Database '" + DB_NAME + "' missing — creating...");
            try (Statement s = c.createStatement()) {
                // Quoting the name preserves the MixedCase form.
                s.executeUpdate("CREATE DATABASE \"" + DB_NAME + "\"");
            }
            log("Database '" + DB_NAME + "' created.");
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to ensure database '" + DB_NAME + "' exists: " + e.getMessage(), e);
        }
    }

    private static void ensureSchemasExist() {
        try (Connection c = openTarget()) {
            int created = 0, existed = 0;
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
                try (Statement create = c.createStatement()) {
                    for (String schema : SCHEMAS) {
                        check.setString(1, schema);
                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next()) {
                                existed++;
                                continue;
                            }
                        }
                        create.executeUpdate(
                                "CREATE SCHEMA IF NOT EXISTS \"" + schema +
                                "\" AUTHORIZATION " + USER);
                        created++;
                    }
                }
            }
            log("Schemas: " + existed + " already existed, " + created + " created.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure schemas exist: " + e.getMessage(), e);
        }
    }

    private static Connection openMaster() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://" + HOST + ":" + PORT + "/postgres", USER, PASSWORD);
    }

    private static Connection openTarget() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME, USER, PASSWORD);
    }

    private static String envOr(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    /** Console + (if not headless) Swing dialog. */
    private static void showPostgresMissingAlert() {
        final String message =
                "PostgreSQL is not reachable at " + HOST + ":" + PORT + ".\n\n" +
                "Please install PostgreSQL 16+ from:\n" +
                "https://www.postgresql.org/download/windows/\n\n" +
                "During install, use:\n" +
                "  Username: postgres\n" +
                "  Password: postgres\n" +
                "  Port:     5432\n\n" +
                "(Or set POSTGRES_HOST, POSTGRES_PORT, POSTGRES_USER,\n" +
                "POSTGRES_PASSWORD env vars to match your install.)\n\n" +
                "Then restart this service.";

        // Loud console first — works even when the dialog can't be shown.
        System.err.println();
        System.err.println("================================================================");
        System.err.println("  POSTGRESQL NOT REACHABLE — see message below");
        System.err.println("================================================================");
        for (String line : message.split("\n")) System.err.println("  " + line);
        System.err.println("================================================================");
        System.err.println();

        try {
            if (GraphicsEnvironment.isHeadless()) return;
            JOptionPane.showMessageDialog(null, message,
                    "ElectDeptSoftware — PostgreSQL Required", JOptionPane.ERROR_MESSAGE);
        } catch (HeadlessException ignored) {
            // No display available; the stderr block above already told them.
        }
    }

    private static void log(String message) {
        System.out.println("[db-bootstrap] " + message);
    }
}
