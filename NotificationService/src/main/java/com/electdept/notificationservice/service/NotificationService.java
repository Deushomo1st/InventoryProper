package com.electdept.notificationservice.service;

import com.electdept.notificationservice.model.Notification;
import com.electdept.notificationservice.model.NotificationTemplate;
import com.electdept.notificationservice.repository.NotificationRepository;
import com.electdept.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final NotificationTemplateRepository templateRepo;

    public NotificationService(NotificationRepository repo, NotificationTemplateRepository templateRepo) {
        this.repo = repo;
        this.templateRepo = templateRepo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getForUser(Long userId, String status) {
        List<Notification> list = (status != null)
            ? repo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status)
            : repo.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        return toDto(repo.findById(id).orElseThrow(() -> new RuntimeException("Notification not found: " + id)));
    }

    @Transactional
    public Notification send(Map<String, Object> body) {
        Notification n = new Notification();
        n.setUserId(((Number) body.get("userId")).longValue());
        n.setSubject((String) body.get("subject"));
        n.setBody((String) body.get("body"));
        if (body.containsKey("channel")) n.setChannel((String) body.get("channel"));
        if (body.containsKey("sourceService")) n.setSourceService((String) body.get("sourceService"));
        if (body.containsKey("sourceId") && body.get("sourceId") != null) {
            n.setSourceId(((Number) body.get("sourceId")).longValue());
        }
        n.setStatus("SENT");
        return repo.save(n);
    }

    /** Render a template with variables, then send. body = {templateCode, userId, variables: {key: value}} */
    @Transactional
    @SuppressWarnings("unchecked")
    public Notification sendFromTemplate(Map<String, Object> body) {
        String templateCode = (String) body.get("templateCode");
        Long userId = ((Number) body.get("userId")).longValue();
        Map<String, Object> variables = (Map<String, Object>) body.getOrDefault("variables", Map.of());

        NotificationTemplate t = templateRepo.findByCode(templateCode)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateCode));

        String subject = substitute(t.getSubjectTemplate(), variables);
        String renderedBody = substitute(t.getBodyTemplate(), variables);

        Notification n = new Notification();
        n.setUserId(userId);
        n.setTemplateCode(templateCode);
        n.setSubject(subject);
        n.setBody(renderedBody);
        n.setChannel(t.getChannel());
        n.setStatus("SENT");
        if (body.containsKey("sourceService")) n.setSourceService((String) body.get("sourceService"));
        if (body.containsKey("sourceId") && body.get("sourceId") != null) {
            n.setSourceId(((Number) body.get("sourceId")).longValue());
        }
        return repo.save(n);
    }

    private String substitute(String template, Map<String, Object> vars) {
        String result = template;
        for (Map.Entry<String, Object> e : vars.entrySet()) {
            result = result.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return result;
    }

    @Transactional
    public Notification markRead(Long id, Long callerUserId) {
        Notification n = repo.findById(id).orElseThrow(() -> new RuntimeException("Notification not found: " + id));
        if (!n.getUserId().equals(callerUserId)) {
            throw new RuntimeException("Cannot mark another user's notification as read");
        }
        n.setStatus("READ");
        n.setReadAt(Instant.now());
        return repo.save(n);
    }

    private Map<String, Object> toDto(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("userId", n.getUserId());
        m.put("templateCode", n.getTemplateCode());
        m.put("subject", n.getSubject());
        m.put("body", n.getBody());
        m.put("channel", n.getChannel());
        m.put("status", n.getStatus());
        m.put("sourceService", n.getSourceService());
        m.put("sourceId", n.getSourceId());
        m.put("createdAt", n.getCreatedAt());
        m.put("readAt", n.getReadAt());
        return m;
    }
}
