package com.electdept.documentmanagementservice.service;

import com.electdept.documentmanagementservice.model.Reviewer;
import com.electdept.documentmanagementservice.model.Document;
import com.electdept.documentmanagementservice.repository.ReviewerRepository;
import com.electdept.documentmanagementservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReviewerService {

    private final ReviewerRepository repo;
    private final DocumentRepository supplierRepo;

    public ReviewerService(ReviewerRepository repo, DocumentRepository supplierRepo) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Reviewer c : repo.findAll()) {
            result.add(toMap(c));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Reviewer c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + id));
        return toMap(c);
    }

    @Transactional
    public Reviewer create(Map<String, Object> body) {
        Reviewer c = new Reviewer();
        applyBody(c, body);
        return repo.save(c);
    }

    @Transactional
    public Reviewer update(Long id, Map<String, Object> body) {
        Reviewer c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + id));
        applyBody(c, body);
        return repo.save(c);
    }

    private void applyBody(Reviewer c, Map<String, Object> body) {
        if (body.containsKey("supplierId")) {
            Long supplierId = body.get("supplierId") != null
                    ? ((Number) body.get("supplierId")).longValue() : null;
            if (supplierId != null) {
                c.setSupplier(supplierRepo.findById(supplierId)
                        .orElseThrow(() -> new RuntimeException("Document not found: " + supplierId)));
            } else {
                c.setSupplier(null);
            }
        }
        if (body.containsKey("name")) c.setName((String) body.get("name"));
        if (body.containsKey("email")) c.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) c.setPhone((String) body.get("phone"));
        if (body.containsKey("role")) c.setRole((String) body.get("role"));
        if (body.containsKey("active")) c.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Reviewer c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reviewer not found: " + id));
        repo.delete(c);
    }

    private Map<String, Object> toMap(Reviewer c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("name", c.getName());
        map.put("email", c.getEmail());
        map.put("phone", c.getPhone());
        map.put("role", c.getRole());
        map.put("active", c.getActive());
        map.put("createdAt", c.getCreatedAt());
        if (c.getSupplier() != null) {
            map.put("supplierId", c.getSupplier().getId());
            map.put("supplierName", c.getSupplier().getName());
        }
        return map;
    }
}
