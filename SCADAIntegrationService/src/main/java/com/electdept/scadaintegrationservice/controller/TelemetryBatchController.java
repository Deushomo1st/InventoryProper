package com.electdept.scadaintegrationservice.controller;

import com.electdept.scadaintegrationservice.service.TelemetryBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/scada/receipts")
public class TelemetryBatchController {

    private final TelemetryBatchService service;

    public TelemetryBatchController(TelemetryBatchService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('receiving.create')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(201).body(service.create(body, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('receiving.create')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('receiving.create')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('receiving.complete')")
    public ResponseEntity<?> complete(@PathVariable Long id, HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return ResponseEntity.ok(service.complete(id, bearer));
    }

    @PutMapping("/lines/{lineId}/qty")
    @PreAuthorize("hasAuthority('receiving.create')")
    public ResponseEntity<?> updateLineQty(@PathVariable Long lineId, @RequestBody Map<String, Object> body) {
        BigDecimal qtyReceived = new BigDecimal(body.get("qtyReceived").toString());
        return ResponseEntity.ok(service.updateLineQty(lineId, qtyReceived));
    }
}
