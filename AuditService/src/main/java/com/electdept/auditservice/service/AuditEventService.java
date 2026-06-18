package com.electdept.auditservice.service;

import com.electdept.auditservice.model.AuditEvent;
import com.electdept.auditservice.repository.AuditEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditEventService {

    private final AuditEventRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public AuditEventService(AuditEventRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAll(Long userId, String eventType, String sourceService) {
        List<AuditEvent> events;
        if (userId != null) {
            events = repo.findByUserIdOrderByCreatedAtDesc(userId);
        } else if (eventType != null) {
            events = repo.findByEventTypeOrderByCreatedAtDesc(eventType);
        } else if (sourceService != null) {
            events = repo.findBySourceServiceOrderByCreatedAtDesc(sourceService);
        } else {
            events = repo.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }
        return events.stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        return toMap(repo.findById(id).orElseThrow(() -> new RuntimeException("AuditEvent not found: " + id)));
    }

    @Transactional
    public AuditEvent create(Map<String, Object> body) {
        AuditEvent e = new AuditEvent();
        e.setUserId(((Number) body.get("userId")).longValue());
        e.setEventType((String) body.get("eventType"));
        e.setSourceService((String) body.get("sourceService"));
        if (body.get("sourceId") != null) e.setSourceId(((Number) body.get("sourceId")).longValue());
        e.setResourceType((String) body.get("resourceType"));
        if (body.get("resourceId") != null) e.setResourceId(((Number) body.get("resourceId")).longValue());
        e.setMetadata((String) body.get("metadata"));
        return repo.save(e);
    }

    /**
     * Ingest rows from inventory.stock_ledger (cross-schema) into audit_event.
     * Idempotent: (source_service, source_id) is UNIQUE; skips rows already ingested.
     */
    @Transactional
    public Map<String, Object> syncLedger() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, sku_id, warehouse_id, bin_id, delta, reason_code, ref_doc_type, ref_doc_id, user_id, created_at " +
            "FROM inventory.stock_ledger ORDER BY id"
        );
        int ingested = 0;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            Long ledgerId = ((Number) row.get("id")).longValue();
            if (repo.existsBySourceServiceAndSourceId("inventory", ledgerId)) {
                skipped++;
                continue;
            }
            AuditEvent e = new AuditEvent();
            e.setUserId(((Number) row.get("user_id")).longValue());
            e.setEventType((String) row.get("reason_code"));
            e.setSourceService("inventory");
            e.setSourceId(ledgerId);
            e.setResourceType((String) row.get("ref_doc_type"));
            Object refDocId = row.get("ref_doc_id");
            if (refDocId != null) e.setResourceId(((Number) refDocId).longValue());
            e.setMetadata(String.format(
                "{\"skuId\":%s,\"warehouseId\":%s,\"binId\":%s,\"delta\":%s}",
                row.get("sku_id"), row.get("warehouse_id"), row.get("bin_id"), row.get("delta")
            ));
            repo.save(e);
            ingested++;
        }
        return Map.of("ingested", ingested, "skipped", skipped, "totalRowsInSource", rows.size());
    }

    private Map<String, Object> toMap(AuditEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("userId", e.getUserId());
        m.put("eventType", e.getEventType());
        m.put("sourceService", e.getSourceService());
        m.put("sourceId", e.getSourceId());
        m.put("resourceType", e.getResourceType());
        m.put("resourceId", e.getResourceId());
        m.put("metadata", e.getMetadata());
        m.put("createdAt", e.getCreatedAt());
        return m;
    }
}
