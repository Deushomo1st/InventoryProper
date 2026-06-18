package com.electdept.energymonitoringservice.controller;

import com.electdept.energymonitoringservice.service.MeterReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/energy/adjustments")
public class AdjustmentController {

    private final MeterReadingService service;

    public AdjustmentController(MeterReadingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication authentication) {
        try {
            Long userId = extractUserId(authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(service.recordAdjustment(body, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }
        // JwtAuthFilter sets principal to Long userId from the JWT sub claim.
        // Previously this hardcoded `return 1L;` which silently attributed every
        // adjustment to Deus, defeating the entire audit traceability story.
        Object p = authentication.getPrincipal();
        if (p instanceof Long) return (Long) p;
        if (p instanceof Number) return ((Number) p).longValue();
        // Fallback: authentication.getName() is the toString of the principal
        try { return Long.parseLong(authentication.getName()); } catch (Exception e) {
            throw new RuntimeException("Unrecognized principal: " + p);
        }
    }
}
