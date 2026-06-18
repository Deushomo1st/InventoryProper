package com.electdept.authpermissionsservice.bootstrap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs db-bootstrap/bootstrap-seed.sql ONCE, after Spring has fully
 * started and Hibernate has materialised every entity into a real
 * table. That ordering matters because the seed file uses INSERT
 * statements that reference auth.role, auth.permission, etc. — those
 * tables only exist after Hibernate's ddl-auto=update pass on first
 * boot.
 *
 * The seed file is idempotent (every insert ends with ON CONFLICT DO
 * NOTHING), so it's safe to run multiple times. We still skip it when
 * admin@electdept.com already exists, just to keep startup quick on
 * subsequent runs.
 *
 * Last step: overwrite the admin's password_hash via pgcrypto so it
 * decodes to the literal string "password". The static BCrypt hash in
 * the SQL file proved unreliable across BCrypt library versions, so
 * we let Postgres regenerate one at runtime.
 */
@Component
public class SeedRunner {

    private final JdbcTemplate jdbc;

    public SeedRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfNeeded() {
        log("Checking seed state...");

        // 1. Run the seed FILE only on a truly fresh DB (no admin@electdept.com).
        //    Once admin@electdept.com exists we trust the seed has run; the file
        //    is idempotent (ON CONFLICT DO NOTHING) but re-running it at every
        //    boot wastes a couple seconds of startup time.
        if (!userExists("admin@electdept.com")) {
            runSeedFile();
        } else {
            log("Seed file already loaded (admin@electdept.com present); skipping.");
        }

        // 2. Always make sure admin@electdept.com's password hash decodes to
        //    "password", regardless of whether the seed file just ran or not.
        refreshAdminPassword();

        // 3. Ensure the simple-login default admin account exists. Independent
        //    from the seed file so it survives the early-return above on
        //    subsequent boots, and so dev can hop in via "Admin"/"admin123456"
        //    without needing the @-email form.
        ensureDefaultAdminAccount();
    }

    /* ----------------------- helpers ----------------------- */

    /** True if any user with this email already exists. */
    private boolean userExists(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM auth.app_user WHERE email = ?",
                Integer.class, email);
        return count != null && count > 0;
    }

    /**
     * Reads db-bootstrap/bootstrap-seed.sql off disk and runs every
     * statement in it. Skips silently if the file isn't found (the user
     * may have deleted it after first boot — we already have what we
     * need).
     */
    private void runSeedFile() {
        Path seedPath = findSeedFile();
        if (seedPath == null) {
            log("WARNING: bootstrap-seed.sql not found in db-bootstrap/. " +
                "Skipping seed file — roles/permissions/templates won't be loaded. " +
                "You can run it manually via psql later.");
            return;
        }
        log("Running seed file: " + seedPath);
        try {
            String script = Files.readString(seedPath, StandardCharsets.UTF_8);
            int executed = runScript(script);
            log("Seed file completed — executed " + executed + " statement(s).");
        } catch (IOException e) {
            log("ERROR: could not read seed file: " + e.getMessage());
        }
    }

    /**
     * Creates a default "Admin"/"admin123456" account if it isn't already in
     * the DB, and grants it the ADMIN role so it has every permission. The
     * password is hashed at runtime with pgcrypto's bcrypt — same format
     * Spring Security's BCryptPasswordEncoder writes, so login will work.
     *
     * Idempotent: re-running this on a DB that already has the Admin user
     * is a no-op (no double insert, no error).
     *
     * Dev convenience only — strip or change the credentials before any
     * non-local deployment.
     */
    private void ensureDefaultAdminAccount() {
        final String email = "Admin@gmail.com";
        final String rawPassword = "admin123456";

        if (userExists(email)) {
            log("Default '" + email + "' account already present; leaving it as-is.");
            return;
        }

        log("Default '" + email + "' account missing — creating it now.");
        try {
            // pgcrypto is also required for the runtime password refresh above.
            // CREATE EXTENSION IF NOT EXISTS is safe to re-run.
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");

            jdbc.update(
                    "INSERT INTO auth.app_user " +
                    "(email, password_hash, active, created_at, manager) " +
                    "VALUES (?, crypt(?, gen_salt('bf', 10)), true, now(), true)",
                    email, rawPassword);

            // Grant the ADMIN role if it exists (it should, since the seed
            // file ran above on a fresh DB). On a stripped-down install
            // where the seed didn't run, ADMIN may not exist — log and skip
            // rather than fail; the user can still log in, they'll just
            // hit permission walls until a role is assigned.
            int linked = jdbc.update(
                    "INSERT INTO auth.user_role (user_id, role_id, granted_at) " +
                    "SELECT u.id, r.id, now() " +
                    "FROM auth.app_user u, auth.role r " +
                    "WHERE u.email = ? AND r.name = ? " +
                    "  AND NOT EXISTS (" +
                    "    SELECT 1 FROM auth.user_role ur " +
                    "    WHERE ur.user_id = u.id AND ur.role_id = r.id)",
                    email, "ADMIN");

            if (linked == 0) {
                log("  WARN: ADMIN role not found — '" + email +
                    "' was created with NO roles. Run the seed file manually " +
                    "or assign a role via the Users page after first login.");
            }

            log("Default account ready:");
            log("  email:    " + email);
            log("  password: " + rawPassword);
            log("  role:     ADMIN");
        } catch (Exception e) {
            log("ERROR: could not create default '" + email + "' account: " + e.getMessage());
        }
    }

    /** Walks up from the working directory looking for db-bootstrap/. */
    private Path findSeedFile() {
        Path[] candidates = {
                Paths.get("db-bootstrap", "bootstrap-seed.sql"),
                Paths.get("..", "db-bootstrap", "bootstrap-seed.sql"),
                Paths.get("..", "..", "db-bootstrap", "bootstrap-seed.sql"),
                Paths.get(System.getProperty("user.dir"), "db-bootstrap", "bootstrap-seed.sql")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p.toAbsolutePath().normalize();
        }
        return null;
    }

    /**
     * Replaces the admin user's password hash with a freshly-generated
     * pgcrypto BCrypt hash of the literal string "password". Works around
     * brittle static hash values in seed files.
     */
    private void refreshAdminPassword() {
        try {
            jdbc.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            jdbc.update(
                    "UPDATE auth.app_user " +
                    "SET password_hash = crypt(?, gen_salt('bf', 10)) " +
                    "WHERE email = ?",
                    "password", "admin@electdept.com");
        } catch (Exception e) {
            log("WARNING: could not refresh admin password hash: " + e.getMessage());
        }
    }

    /**
     * Splits the SQL script into individual statements and runs them.
     * Handles:
     *   - line comments (-- ...)
     *   - block comments (/* ... *&#47;)
     *   - dollar-quoted blocks ($$ ... $$) — these survive intact for
     *     PL/pgSQL DO blocks
     *   - psql meta-commands (lines starting with \) — skipped silently
     *
     * Each non-empty statement is sent to JdbcTemplate as a separate call.
     * Returns the count of statements executed.
     */
    private int runScript(String script) {
        List<String> statements = splitStatements(script);
        int executed = 0;
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) continue;
            try {
                jdbc.execute(trimmed);
                executed++;
            } catch (Exception e) {
                // Don't abort the whole seed for one bad statement — the file
                // is meant to be idempotent, but conflicts on tables that
                // don't exist yet should not stop the rest from running.
                log("  WARN statement failed: " + abbreviate(trimmed) +
                        " — " + e.getMessage());
            }
        }
        return executed;
    }

    private static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollar = false;
        String[] lines = script.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!inDollar) {
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("--")) continue;
                if (trimmed.startsWith("\\")) continue;  // psql meta-command
            }
            // Toggle dollar-quote state on each $$ occurrence on this line.
            int dollarCount = countOccurrences(line, "$$");
            for (int i = 0; i < dollarCount; i++) inDollar = !inDollar;

            current.append(line).append('\n');
            if (!inDollar && trimmed.endsWith(";")) {
                out.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.toString().trim().length() > 0) out.add(current.toString());
        return out;
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    private static String abbreviate(String s) {
        s = s.replaceAll("\\s+", " ");
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    private static void log(String message) {
        System.out.println("[seed-runner] " + message);
    }
}
