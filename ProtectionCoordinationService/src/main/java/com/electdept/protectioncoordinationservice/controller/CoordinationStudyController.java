package com.electdept.protectioncoordinationservice.controller;

import com.electdept.protectioncoordinationservice.service.CoordinationStudyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/protection/studies")
public class CoordinationStudyController {
    private final CoordinationStudyService service;
    
    public CoordinationStudyController(CoordinationStudyService service) { 
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
    @PreAuthorize("hasAuthority('so.create')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(201).body(service.create(body, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('so.create')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('so.create')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('so.confirm')")
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('so.confirm')")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String reason = body != null ? (String) body.get("reason") : null;
        return ResponseEntity.ok(service.cancel(id, reason));
    }
}
