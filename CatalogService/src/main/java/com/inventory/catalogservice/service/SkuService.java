package com.inventory.catalogservice.service;

import com.inventory.catalogservice.model.Sku;
import com.inventory.catalogservice.repository.ProductRepository;
import com.inventory.catalogservice.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SkuService {

    private final SkuRepository repo;
    private final ProductRepository productRepo;

    public SkuService(SkuRepository repo, ProductRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sku s : repo.findAll()) {
            result.add(toMap(s));
        }
        return result;
    }

    public List<Map<String, Object>> getByProductId(Long productId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sku s : repo.findByProductId(productId)) {
            result.add(toMap(s));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Sku s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        return toMap(s);
    }

    @Transactional
    public Sku create(Map<String, Object> body) {
        Sku s = new Sku();
        applyBody(s, body);
        return repo.save(s);
    }

    @Transactional
    public Sku update(Long id, Map<String, Object> body) {
        Sku s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        applyBody(s, body);
        return repo.save(s);
    }

    private void applyBody(Sku s, Map<String, Object> body) {
        if (body.containsKey("skuCode")) s.setSkuCode((String) body.get("skuCode"));
        if (body.containsKey("name")) s.setName((String) body.get("name"));
        if (body.containsKey("active")) s.setActive((Boolean) body.get("active"));
        if (body.containsKey("uomId")) {
            s.setUomId(body.get("uomId") != null
                    ? ((Number) body.get("uomId")).longValue() : null);
        }
        if (body.containsKey("productId")) {
            Long prodId = ((Number) body.get("productId")).longValue();
            s.setProduct(productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + prodId)));
        }
    }

    @Transactional
    public void delete(Long id) {
        Sku s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        repo.delete(s);
    }

    private Map<String, Object> toMap(Sku s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("skuCode", s.getSkuCode());
        map.put("name", s.getName());
        map.put("active", s.getActive());
        map.put("uomId", s.getUomId());
        map.put("createdAt", s.getCreatedAt());
        if (s.getProduct() != null) {
            map.put("productId", s.getProduct().getId());
            map.put("productName", s.getProduct().getName());
        }
        return map;
    }
}