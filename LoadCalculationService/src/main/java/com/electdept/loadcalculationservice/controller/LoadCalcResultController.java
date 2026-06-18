package com.electdept.loadcalculationservice.controller;

import com.electdept.loadcalculationservice.service.LoadCalcResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/loadcalc/results")
public class LoadCalcResultController {

    private final LoadCalcResultService service;

    public LoadCalcResultController(LoadCalcResultService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('replenishment.run')")
    public ResponseEntity<?> sync(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return ResponseEntity.ok(service.runSync(bearer));
    }

    @PostMapping("/{id}/order")
    @PreAuthorize("hasAuthority('replenishment.order')")
    public ResponseEntity<?> order(@PathVariable Long id, Authentication auth, HttpServletRequest req) {
        Long userId = (Long) auth.getPrincipal();
        String bearer = req.getHeader("Authorization");
        return ResponseEntity.ok(service.order(id, userId, bearer));
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAuthority('replenishment.order')")
    public ResponseEntity<?> dismiss(@PathVariable Long id) {
        return ResponseEntity.ok(service.dismiss(id));
    }
}
