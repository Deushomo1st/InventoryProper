package com.electdept.authpermissionsservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Best-effort writer to AuditService. Used to record privileged auth actions
 * (role grants/revokes, permission changes, etc.) into the unified audit log.
 *
 * Failures are swallowed and logged — we never block the primary auth action
 * just because the audit sink is down. The system stays operational; the
 * audit row just doesn't appear, which is recoverable later via a re-sync
 * job if/when one exists.
 */
@Component
public class AuditClient {

    private final String auditUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public AuditClient(@Value("${services.audit.url:http://localhost:9012}") String auditUrl) {
        this.auditUrl = auditUrl;
    }

    public void writeEvent(String bearerToken,
                           Long userId,
                           String eventType,
                           String sourceService,
                           Long sourceId,
                           String resourceType,
                           Long resourceId,
                           String metadataJson) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", userId);
            body.put("eventType", eventType);
            body.put("sourceService", sourceService);
            if (sourceId != null) body.put("sourceId", sourceId);
            if (resourceType != null) body.put("resourceType", resourceType);
            if (resourceId != null) body.put("resourceId", resourceId);
            if (metadataJson != null) body.put("metadata", metadataJson);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(auditUrl + "/audit/events", request, Map.class);
        } catch (Exception e) {
            // Swallow: audit writes never block the primary action.
            System.err.println("AuditClient.writeEvent failed (best-effort): " + e.getMessage());
        }
    }
}
