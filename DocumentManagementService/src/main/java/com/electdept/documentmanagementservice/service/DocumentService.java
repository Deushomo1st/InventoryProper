package com.electdept.documentmanagementservice.service;

import com.electdept.documentmanagementservice.model.Document;
import com.electdept.documentmanagementservice.repository.ReviewerRepository;
import com.electdept.documentmanagementservice.repository.DocumentRevisionRepository;
import com.electdept.documentmanagementservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DocumentService {

    private final DocumentRepository repo;
    private final ReviewerRepository contactRepo;
    private final DocumentRevisionRepository priceListRepo;

    public DocumentService(DocumentRepository repo, ReviewerRepository contactRepo, DocumentRevisionRepository priceListRepo) {
        this.repo = repo;
        this.contactRepo = contactRepo;
        this.priceListRepo = priceListRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document s : repo.findAll()) {
            result.add(toMap(s));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Document s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        return toMap(s);
    }

    @Transactional
    public Document create(Map<String, Object> body) {
        Document s = new Document();
        applyBody(s, body);
        if (s.getCode() == null || s.getCode().isEmpty()) {
            s.setCode(generateCode());
        }
        return repo.save(s);
    }

    @Transactional
    public Document update(Long id, Map<String, Object> body) {
        Document s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        applyBody(s, body);
        return repo.save(s);
    }

    private void applyBody(Document s, Map<String, Object> body) {
        if (body.containsKey("code")) s.setCode((String) body.get("code"));
        if (body.containsKey("name")) s.setName((String) body.get("name"));
        if (body.containsKey("taxId")) s.setTaxId((String) body.get("taxId"));
        if (body.containsKey("address")) s.setAddress((String) body.get("address"));
        if (body.containsKey("status")) s.setStatus((String) body.get("status"));
        if (body.containsKey("active")) s.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Document s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        priceListRepo.deleteAllBySupplierId(id);
        contactRepo.deleteAllBySupplierId(id);
        repo.delete(s);
    }

    private String generateCode() {
        int max = 0;
        for (Document s : repo.findAll()) {
            String code = s.getCode();
            if (code != null && code.startsWith("SUP-")) {
                try {
                    int num = Integer.parseInt(code.substring(4));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("SUP-%03d", max + 1);
    }

    private Map<String, Object> toMap(Document s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("code", s.getCode());
        map.put("name", s.getName());
        map.put("taxId", s.getTaxId());
        map.put("address", s.getAddress());
        map.put("status", s.getStatus());
        map.put("active", s.getActive());
        map.put("createdAt", s.getCreatedAt());
        return map;
    }
}
