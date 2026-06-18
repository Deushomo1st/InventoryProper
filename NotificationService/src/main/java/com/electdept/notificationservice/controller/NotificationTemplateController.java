package com.electdept.notificationservice.controller;

import com.electdept.notificationservice.service.NotificationTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification/templates")
public class NotificationTemplateController {

    private final NotificationTemplateService service;
    public NotificationTemplateController(NotificationTemplateService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<?> byCode(@PathVariable String code) { return ResponseEntity.ok(service.getByCode(code)); }

    @PostMapping
    @PreAuthorize("hasAuthority('notification.send')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) { return ResponseEntity.status(201).body(service.create(body)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('notification.send')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) { return ResponseEntity.ok(service.update(id, body)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notification.send')")
    public ResponseEntity<?> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.ok(Map.of("message", "Deleted")); }
}
