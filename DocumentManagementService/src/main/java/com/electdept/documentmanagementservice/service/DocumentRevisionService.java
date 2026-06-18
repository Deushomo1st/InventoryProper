package com.electdept.documentmanagementservice.service;

import com.electdept.documentmanagementservice.model.DocumentRevision;
import com.electdept.documentmanagementservice.model.Document;
import com.electdept.documentmanagementservice.repository.DocumentRevisionRepository;
import com.electdept.documentmanagementservice.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class DocumentRevisionService {

    private final DocumentRevisionRepository repo;
    private final DocumentRepository supplierRepo;

    public DocumentRevisionService(DocumentRevisionRepository repo, DocumentRepository supplierRepo) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocumentRevision p : repo.findAll()) {
            result.add(toMap(p));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        DocumentRevision p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("DocumentRevision not found: " + id));
        return toMap(p);
    }

    @Transactional
    public DocumentRevision create(Map<String, Object> body) {
        DocumentRevision p = new DocumentRevision();
        applyBody(p, body);
        return repo.save(p);
    }

    @Transactional
    public DocumentRevision update(Long id, Map<String, Object> body) {
        DocumentRevision p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("DocumentRevision not found: " + id));
        applyBody(p, body);
        return repo.save(p);
    }

    private void applyBody(DocumentRevision p, Map<String, Object> body) {
        if (body.containsKey("supplierId")) {
            Long supplierId = body.get("supplierId") != null
                    ? ((Number) body.get("supplierId")).longValue() : null;
            if (supplierId != null) {
                p.setSupplier(supplierRepo.findById(supplierId)
                        .orElseThrow(() -> new RuntimeException("Document not found: " + supplierId)));
            } else {
                p.setSupplier(null);
            }
        }
        if (body.containsKey("skuId")) {
            p.setSkuId(body.get("skuId") != null
                    ? ((Number) body.get("skuId")).longValue() : null);
        }
        if (body.containsKey("unitPrice")) {
            p.setUnitPrice(new BigDecimal(body.get("unitPrice").toString()));
        }
        if (body.containsKey("currency")) p.setCurrency((String) body.get("currency"));
        if (body.containsKey("validFrom")) {
            p.setValidFrom(body.get("validFrom") != null
                    ? LocalDate.parse(body.get("validFrom").toString()) : null);
        }
        if (body.containsKey("validTo")) {
            p.setValidTo(body.get("validTo") != null
                    ? LocalDate.parse(body.get("validTo").toString()) : null);
        }
        if (body.containsKey("active")) p.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        DocumentRevision p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("DocumentRevision not found: " + id));
        repo.delete(p);
    }

    private Map<String, Object> toMap(DocumentRevision p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("skuId", p.getSkuId());
        map.put("unitPrice", p.getUnitPrice());
        map.put("currency", p.getCurrency());
        map.put("validFrom", p.getValidFrom());
        map.put("validTo", p.getValidTo());
        map.put("active", p.getActive());
        map.put("createdAt", p.getCreatedAt());
        if (p.getSupplier() != null) {
            map.put("supplierId", p.getSupplier().getId());
            map.put("supplierName", p.getSupplier().getName());
        }
        return map;
    }
}
