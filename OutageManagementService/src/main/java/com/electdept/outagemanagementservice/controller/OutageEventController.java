package com.electdept.outagemanagementservice.controller;

import com.electdept.outagemanagementservice.service.OutageEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/outage/events")
public class OutageEventController {
    private final OutageEventService service;

    public OutageEventController(OutageEventService service) {
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

    @GetMapping("/by-so/{soId}")
    public ResponseEntity<?> bySo(@PathVariable Long soId) {
        return ResponseEntity.ok(service.getBySalesOrderId(soId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('shipment.create')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(201).body(service.create(body, userId));
    }

    @PutMapping("/lines/{lineId}/qty")
    @PreAuthorize("hasAuthority('shipment.create')")
    public ResponseEntity<?> updateLineQty(@PathVariable Long lineId, @RequestBody Map<String, Object> body) {
        BigDecimal qty = new BigDecimal(body.get("qtyShipped").toString());
        Long binId = body.get("binId") != null ? ((Number) body.get("binId")).longValue() : null;
        return ResponseEntity.ok(service.updateLineQty(lineId, qty, binId));
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('shipment.ship')")
    public ResponseEntity<?> ship(@PathVariable Long id, HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return ResponseEntity.ok(service.ship(id, bearer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('shipment.create')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
