package com.electdept.billingtariffservice.service;

import com.electdept.billingtariffservice.model.RateAdjustment;
import com.electdept.billingtariffservice.repository.RateAdjustmentRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RateAdjustmentService {

    private final RateAdjustmentRepository repo;

    public RateAdjustmentService(RateAdjustmentRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        RateAdjustment discount = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("RateAdjustment not found: " + id));
        return toDto(discount);
    }

    public Map<String, Object> create(Map<String, Object> body) {
        RateAdjustment discount = new RateAdjustment();
        discount.setCode((String) body.get("code"));
        discount.setName((String) body.get("name"));
        discount.setDiscountType((String) body.get("discountType"));
        discount.setValue(new java.math.BigDecimal(body.get("value").toString()));
        discount.setSkuId(body.get("skuId") != null ? ((Number) body.get("skuId")).longValue() : null);
        discount.setMinQty(body.get("minQty") != null ? new java.math.BigDecimal(body.get("minQty").toString()) : new java.math.BigDecimal("1"));
        discount.setValidFrom(body.get("validFrom") != null ? java.time.LocalDate.parse((String) body.get("validFrom")) : null);
        discount.setValidTo(body.get("validTo") != null ? java.time.LocalDate.parse((String) body.get("validTo")) : null);
        discount.setActive(body.get("active") != null ? (Boolean) body.get("active") : true);
        
        discount = repo.save(discount);
        return toDto(discount);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        RateAdjustment discount = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("RateAdjustment not found: " + id));
        
        if (body.containsKey("code")) discount.setCode((String) body.get("code"));
        if (body.containsKey("name")) discount.setName((String) body.get("name"));
        if (body.containsKey("discountType")) discount.setDiscountType((String) body.get("discountType"));
        if (body.containsKey("value")) discount.setValue(new java.math.BigDecimal(body.get("value").toString()));
        if (body.containsKey("skuId")) discount.setSkuId(body.get("skuId") != null ? ((Number) body.get("skuId")).longValue() : null);
        if (body.containsKey("minQty")) discount.setMinQty(new java.math.BigDecimal(body.get("minQty").toString()));
        if (body.containsKey("validFrom")) discount.setValidFrom(java.time.LocalDate.parse((String) body.get("validFrom")));
        if (body.containsKey("validTo")) discount.setValidTo(java.time.LocalDate.parse((String) body.get("validTo")));
        if (body.containsKey("active")) discount.setActive((Boolean) body.get("active"));
        
        discount = repo.save(discount);
        return toDto(discount);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private Map<String, Object> toDto(RateAdjustment d) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", d.getId());
        dto.put("code", d.getCode());
        dto.put("name", d.getName());
        dto.put("discountType", d.getDiscountType());
        dto.put("value", d.getValue());
        dto.put("skuId", d.getSkuId());
        dto.put("minQty", d.getMinQty());
        dto.put("validFrom", d.getValidFrom());
        dto.put("validTo", d.getValidTo());
        dto.put("active", d.getActive());
        dto.put("createdAt", d.getCreatedAt());
        return dto;
    }
}
