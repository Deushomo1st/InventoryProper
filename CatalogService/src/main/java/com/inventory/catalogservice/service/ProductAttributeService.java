package com.inventory.catalogservice.service;

import com.inventory.catalogservice.model.ProductAttribute;
import com.inventory.catalogservice.repository.ProductAttributeRepository;
import com.inventory.catalogservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProductAttributeService {

    private final ProductAttributeRepository repo;
    private final ProductRepository productRepo;

    public ProductAttributeService(ProductAttributeRepository repo, ProductRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    public List<Map<String, Object>> getByProductId(Long productId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductAttribute a : repo.findByProductId(productId)) {
            result.add(toMap(a));
        }
        return result;
    }

    @Transactional
    public ProductAttribute create(Map<String, Object> body) {
        ProductAttribute a = new ProductAttribute();
        applyBody(a, body);
        return repo.save(a);
    }

    @Transactional
    public ProductAttribute update(Long id, Map<String, Object> body) {
        ProductAttribute a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        applyBody(a, body);
        return repo.save(a);
    }

    private void applyBody(ProductAttribute a, Map<String, Object> body) {
        if (body.containsKey("key")) a.setKey((String) body.get("key"));
        if (body.containsKey("value")) a.setValue((String) body.get("value"));
        if (body.containsKey("productId")) {
            Long prodId = ((Number) body.get("productId")).longValue();
            a.setProduct(productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + prodId)));
        }
    }

    @Transactional
    public void delete(Long id) {
        ProductAttribute a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        repo.delete(a);
    }

    private Map<String, Object> toMap(ProductAttribute a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("key", a.getKey());
        map.put("value", a.getValue());
        if (a.getProduct() != null) {
            map.put("productId", a.getProduct().getId());
        }
        return map;
    }
}