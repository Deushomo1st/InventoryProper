package com.electdept.circuitdesignservice.controller;

import com.electdept.circuitdesignservice.service.CircuitDiagramService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/circuit/diagrams")
public class CircuitDiagramController {
    private final CircuitDiagramService service;

    public CircuitDiagramController(CircuitDiagramService service) {
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
    @PreAuthorize("hasAuthority('movement.create')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(201).body(service.create(body, userId));
    }

    @PutMapping("/lines/{lineId}")
    @PreAuthorize("hasAuthority('movement.create')")
    public ResponseEntity<?> updateLine(@PathVariable Long lineId, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.updateLine(lineId, body));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('movement.execute')")
    public ResponseEntity<?> execute(@PathVariable Long id, HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return ResponseEntity.ok(service.execute(id, bearer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('movement.create')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
