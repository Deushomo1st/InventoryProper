package com.electdept.workorderservice.controller;

import com.electdept.workorderservice.service.WorkOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/workorder/orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
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
    @PreAuthorize("hasAuthority('po.create')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(201).body(service.create(body, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('po.create')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('po.create')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('po.submit')")
    public ResponseEntity<?> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('po.approve')")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        Long approverId = (Long) auth.getPrincipal();
        String reason = body != null ? (String) body.get("reason") : null;
        return ResponseEntity.ok(service.approve(id, approverId, reason));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('po.approve')")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Long approverId = (Long) auth.getPrincipal();
        String reason = (String) body.get("reason");
        return ResponseEntity.ok(service.reject(id, approverId, reason));
    }
}
