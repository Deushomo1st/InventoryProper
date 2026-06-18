package com.electdept.authpermissionsservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Best-effort pusher to NotificationService. Used to deliver in-app
 * notifications when something important happens to a user's account
 * (role granted, permission added, etc.).
 *
 * Failures are swallowed — a missed notification shouldn't block the
 * primary auth action. Worst case the user just won't see an inbox row.
 */
@Component
public class NotificationClient {

    private final String notificationUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public NotificationClient(@Value("${services.notification.url:http://localhost:9014}") String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    /** Render a template + push to the target user. variables map is interpolated into ${var} placeholders. */
    public void sendFromTemplate(String bearerToken,
                                 String templateCode,
                                 Long targetUserId,
                                 Map<String, Object> variables) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("templateCode", templateCode);
            body.put("userId", targetUserId);
            body.put("variables", variables == null ? Map.of() : variables);
            body.put("sourceService", "auth");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(notificationUrl + "/notification/notifications/from-template", request, Map.class);
        } catch (Exception e) {
            System.err.println("NotificationClient.sendFromTemplate failed (best-effort): " + e.getMessage());
        }
    }
}
