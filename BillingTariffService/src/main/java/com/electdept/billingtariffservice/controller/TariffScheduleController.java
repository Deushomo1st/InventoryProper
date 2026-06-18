package com.electdept.billingtariffservice.controller;

import com.electdept.billingtariffservice.service.TariffScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/billing/schedules")
public class TariffScheduleController {

    private final TariffScheduleService service;
    public TariffScheduleController(TariffScheduleService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('price.manage')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('price.manage')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('price.manage')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/{id}/entries")
    @PreAuthorize("hasAuthority('price.manage')")
    public ResponseEntity<?> addEntry(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.addEntry(id, body));
    }

    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasAuthority('price.manage')")
    public ResponseEntity<?> deleteEntry(@PathVariable Long entryId) {
        service.deleteEntry(entryId);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
