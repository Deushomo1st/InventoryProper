package com.electdept.protectioncoordinationservice.controller;

import com.electdept.protectioncoordinationservice.service.ProtectionSchemeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/protection/schemes")
public class ProtectionSchemeController {
    private final ProtectionSchemeService service;
    
    public ProtectionSchemeController(ProtectionSchemeService service) { 
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
    @PreAuthorize("hasAuthority('customer.manage')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer.manage')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customer.manage')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
