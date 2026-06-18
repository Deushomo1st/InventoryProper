package com.electdept.cablesizingservice.service;

import com.electdept.cablesizingservice.model.CableSizingRequest;
import com.electdept.cablesizingservice.repository.CableSizingRequestRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CableSizingRequestService {

    private final CableSizingRequestRepository repo;

    public CableSizingRequestService(CableSizingRequestRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public Map<String, Object> getById(Long id) {
        CableSizingRequest rule = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("CableSizingRequest not found: " + id));
        return toDto(rule);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        CableSizingRequest rule = new CableSizingRequest();
        rule.setSkuId(((Number) body.get("skuId")).longValue());
        rule.setWarehouseId(((Number) body.get("warehouseId")).longValue());
        rule.setReorderPoint(new BigDecimal(body.get("reorderPoint").toString()));
        rule.setReorderQty(new BigDecimal(body.get("reorderQty").toString()));
        
        if (body.containsKey("preferredSupplierId") && body.get("preferredSupplierId") != null) {
            rule.setPreferredSupplierId(((Number) body.get("preferredSupplierId")).longValue());
        }
        
        rule.setDefaultUnitPrice(new BigDecimal(body.get("defaultUnitPrice").toString()));
        
        if (body.containsKey("active")) {
            rule.setActive((Boolean) body.get("active"));
        } else {
            rule.setActive(true);
        }
        
        rule = repo.save(rule);
        return toDto(rule);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CableSizingRequest rule = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("CableSizingRequest not found: " + id));
        
        if (body.containsKey("skuId")) {
            rule.setSkuId(((Number) body.get("skuId")).longValue());
        }
        if (body.containsKey("warehouseId")) {
            rule.setWarehouseId(((Number) body.get("warehouseId")).longValue());
        }
        if (body.containsKey("reorderPoint")) {
            rule.setReorderPoint(new BigDecimal(body.get("reorderPoint").toString()));
        }
        if (body.containsKey("reorderQty")) {
            rule.setReorderQty(new BigDecimal(body.get("reorderQty").toString()));
        }
        if (body.containsKey("preferredSupplierId")) {
            rule.setPreferredSupplierId(body.get("preferredSupplierId") != null ? 
                    ((Number) body.get("preferredSupplierId")).longValue() : null);
        }
        if (body.containsKey("defaultUnitPrice")) {
            rule.setDefaultUnitPrice(new BigDecimal(body.get("defaultUnitPrice").toString()));
        }
        if (body.containsKey("active")) {
            rule.setActive((Boolean) body.get("active"));
        }
        
        rule = repo.save(rule);
        return toDto(rule);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("CableSizingRequest not found: " + id);
        }
        repo.deleteById(id);
    }

    private Map<String, Object> toDto(CableSizingRequest rule) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rule.getId());
        map.put("skuId", rule.getSkuId());
        map.put("warehouseId", rule.getWarehouseId());
        map.put("reorderPoint", rule.getReorderPoint());
        map.put("reorderQty", rule.getReorderQty());
        map.put("preferredSupplierId", rule.getPreferredSupplierId());
        map.put("defaultUnitPrice", rule.getDefaultUnitPrice());
        map.put("active", rule.getActive());
        map.put("createdAt", rule.getCreatedAt());
        return map;
    }
}
