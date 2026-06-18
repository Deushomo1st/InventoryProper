package com.electdept.auditservice.controller;

import com.electdept.auditservice.service.AuditEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/audit/events")
public class AuditEventController {

    private final AuditEventService service;
    public AuditEventController(AuditEventService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAuthority('audit.read')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String sourceService) {
        return ResponseEntity.ok(service.getAll(userId, eventType, sourceService));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit.read')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('audit.write')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(body));
    }

    @PostMapping("/sync/ledger")
    @PreAuthorize("hasAuthority('audit.write')")
    public ResponseEntity<?> syncLedger() {
        return ResponseEntity.ok(service.syncLedger());
    }
}
