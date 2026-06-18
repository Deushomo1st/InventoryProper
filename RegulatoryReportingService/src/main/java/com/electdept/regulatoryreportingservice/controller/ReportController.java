package com.electdept.regulatoryreportingservice.controller;

import com.electdept.regulatoryreportingservice.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reporting")
public class ReportController {

    private final ReportService service;
    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/stock-summary")
    @PreAuthorize("hasAuthority('reports.read')")
    public ResponseEntity<?> stockSummary() { return ResponseEntity.ok(service.stockSummary()); }

    @GetMapping("/orders-summary")
    @PreAuthorize("hasAuthority('reports.read')")
    public ResponseEntity<?> ordersSummary() { return ResponseEntity.ok(service.ordersSummary()); }

    @GetMapping("/activity-by-user")
    @PreAuthorize("hasAuthority('reports.read')")
    public ResponseEntity<?> activityByUser() { return ResponseEntity.ok(service.activityByUser()); }

    @GetMapping("/recent-ledger")
    @PreAuthorize("hasAuthority('reports.read')")
    public ResponseEntity<?> recentLedger(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(service.recentLedger(limit));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('reports.read')")
    public ResponseEntity<?> dashboard() { return ResponseEntity.ok(service.dashboard()); }
}
