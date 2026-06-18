package com.electdept.authpermissionsservice;

import com.electdept.authpermissionsservice.bootstrap.DatabaseBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthPermissionsServiceApplication {

    public static void main(String[] args) {
        // Pre-Spring: confirm PostgreSQL is reachable, create the
        // ElectDeptSoftware database if it's missing, and ensure all 17
        // schemas exist. If Postgres isn't installed/running, this throws
        // and shows a Swing dialog with the download link.
        // See db-bootstrap/README.md for the full flow + env vars.
        DatabaseBootstrap.ensurePreSpring();

        SpringApplication.run(AuthPermissionsServiceApplication.class, args);
    }
}
