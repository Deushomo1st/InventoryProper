package com.electdept.notificationservice.controller;

import com.electdept.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification/notifications")
public class NotificationController {

    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/me")
    public ResponseEntity<?> myNotifications(@RequestParam(required = false) String status, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.getForUser(userId, status));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('notification.read.all')")
    public ResponseEntity<?> list() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) { return ResponseEntity.ok(service.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('notification.send')")
    public ResponseEntity<?> send(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.send(body));
    }

    @PostMapping("/from-template")
    @PreAuthorize("hasAuthority('notification.send')")
    public ResponseEntity<?> sendFromTemplate(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(201).body(service.sendFromTemplate(body));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(service.markRead(id, userId));
    }
}
