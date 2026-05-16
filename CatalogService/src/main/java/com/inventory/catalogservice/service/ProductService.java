package com.inventory.catalogservice.service;

import com.inventory.catalogservice.model.Product;
import com.inventory.catalogservice.repository.CategoryRepository;
import com.inventory.catalogservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final CategoryRepository categoryRepo;

    public ProductService(ProductRepository repo, CategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : repo.findAll()) {
            result.add(toMap(p));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        return toMap(p);
    }

    @Transactional
    public Product create(Map<String, Object> body) {
        Product p = new Product();
        applyBody(p, body);
        // Always auto-generate the code on create. Even if the request body
        // smuggled a `code` field, we ignore it — there's no manual entry on
        // the create UI. This guarantees serial codes never collide with
        // hand-typed ones.
        p.setCode(nextSerialCode(0));
        return repo.save(p);
    }

    @Transactional
    public List<Product> createBulk(Map<String, Object> body, int quantity) {
        if (quantity < 1) quantity = 1;
        if (quantity > 100) quantity = 100;     // sanity cap

        int start = findMaxSerial();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            Product p = new Product();
            applyBody(p, body);
            p.setCode(String.format("PROD-%04d", start + 1 + i));
            products.add(p);
        }
        return repo.saveAll(products);
    }

    /**
     * Highest existing serial number across PROD-XXXX codes.
     * Returns 0 if no serial-coded products exist yet.
     */
    private int findMaxSerial() {
        int max = 0;
        for (Product p : repo.findAll()) {
            String c = p.getCode();
            if (c != null && c.startsWith("PROD-")) {
                try {
                    int n = Integer.parseInt(c.substring(5));
                    if (n > max) max = n;
                } catch (NumberFormatException ignored) {
                    // Non-numeric suffix — ignore, can't be a serial.
                }
            }
        }
        return max;
    }

    /**
     * @param offset additional increment beyond the current max
     */
    private String nextSerialCode(int offset) {
        return String.format("PROD-%04d", findMaxSerial() + 1 + offset);
    }

    @Transactional
    public Product update(Long id, Map<String, Object> body) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        applyBody(p, body);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    private void applyBody(Product p, Map<String, Object> body) {
        if (body.containsKey("code")) p.setCode((String) body.get("code"));
        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("brand")) p.setBrand((String) body.get("brand"));
        if (body.containsKey("description")) p.setDescription((String) body.get("description"));
        if (body.containsKey("active")) p.setActive((Boolean) body.get("active"));
        if (body.containsKey("categoryId")) {
            Long catId = body.get("categoryId") != null
                    ? ((Number) body.get("categoryId")).longValue() : null;
            if (catId != null) {
                p.setCategory(categoryRepo.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Category not found: " + catId)));
            } else {
                p.setCategory(null);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
        repo.delete(p);
    }

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("code", p.getCode());
        map.put("name", p.getName());
        map.put("brand", p.getBrand());
        map.put("description", p.getDescription());
        map.put("active", p.getActive());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());
        if (p.getCategory() != null) {
            map.put("categoryId", p.getCategory().getId());
            map.put("categoryName", p.getCategory().getName());
        }
        return map;
    }
}