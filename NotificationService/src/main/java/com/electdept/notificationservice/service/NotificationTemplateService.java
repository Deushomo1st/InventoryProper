package com.electdept.notificationservice.service;

import com.electdept.notificationservice.model.NotificationTemplate;
import com.electdept.notificationservice.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository repo;

    public NotificationTemplateService(NotificationTemplateRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        NotificationTemplate tpl = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification template not found: " + id));
        return toDto(tpl);
    }

    public Map<String, Object> getByCode(String code) {
        NotificationTemplate tpl = repo.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Notification template not found: " + code));
        return toDto(tpl);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        NotificationTemplate tpl = new NotificationTemplate();
        tpl.setCode((String) body.get("code"));
        tpl.setSubjectTemplate((String) body.get("subjectTemplate"));
        tpl.setBodyTemplate((String) body.get("bodyTemplate"));
        tpl.setChannel(body.get("channel") != null ? (String) body.get("channel") : "IN_APP");
        tpl.setActive(body.get("active") != null ? (Boolean) body.get("active") : true);
        
        tpl = repo.save(tpl);
        return toDto(tpl);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        NotificationTemplate tpl = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification template not found: " + id));
        
        if (body.containsKey("code")) tpl.setCode((String) body.get("code"));
        if (body.containsKey("subjectTemplate")) tpl.setSubjectTemplate((String) body.get("subjectTemplate"));
        if (body.containsKey("bodyTemplate")) tpl.setBodyTemplate((String) body.get("bodyTemplate"));
        if (body.containsKey("channel")) tpl.setChannel((String) body.get("channel"));
        if (body.containsKey("active")) tpl.setActive((Boolean) body.get("active"));
        
        tpl = repo.save(tpl);
        return toDto(tpl);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private Map<String, Object> toDto(NotificationTemplate tpl) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", tpl.getId());
        dto.put("code", tpl.getCode());
        dto.put("subjectTemplate", tpl.getSubjectTemplate());
        dto.put("bodyTemplate", tpl.getBodyTemplate());
        dto.put("channel", tpl.getChannel());
        dto.put("active", tpl.getActive());
        dto.put("createdAt", tpl.getCreatedAt());
        return dto;
    }
}
