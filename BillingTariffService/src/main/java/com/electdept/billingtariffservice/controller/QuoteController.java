package com.electdept.billingtariffservice.controller;

import com.electdept.billingtariffservice.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
public class QuoteController {

    private final QuoteService service;
    public QuoteController(QuoteService service) { this.service = service; }

    @PostMapping("/quote")
    @PreAuthorize("hasAuthority('price.quote')")
    public ResponseEntity<?> quote(@RequestBody Map<String, Object> body) {
        String tier = (String) body.get("customerTier");
        Long skuId = ((Number) body.get("skuId")).longValue();
        BigDecimal qty = new BigDecimal(body.get("qty").toString());
        return ResponseEntity.ok(service.quote(tier, skuId, qty));
    }
}
