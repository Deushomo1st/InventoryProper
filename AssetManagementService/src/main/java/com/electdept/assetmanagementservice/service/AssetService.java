package com.electdept.assetmanagementservice.service;

import com.electdept.assetmanagementservice.model.AssetModel;
import com.electdept.assetmanagementservice.model.Asset;
import com.electdept.assetmanagementservice.repository.AssetModelRepository;
import com.electdept.assetmanagementservice.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AssetService {

    private final AssetRepository repo;
    private final AssetModelRepository productRepo;

    public AssetService(AssetRepository repo, AssetModelRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Asset s : repo.findAll()) {
            result.add(toMap(s));
        }
        return result;
    }

    public List<Map<String, Object>> getByProductId(Long productId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Asset s : repo.findByProductId(productId)) {
            result.add(toMap(s));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Asset s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        return toMap(s);
    }

    @Transactional
    public Asset create(Map<String, Object> body) {
        Asset s = new Asset();
        applyBody(s, body);
        // Auto-generate the sku_code on create. Existing rows keep whatever
        // code they already had — only NEW rows go through this path.
        s.setSkuCode(generateCode(s));
        return repo.save(s);
    }

    @Transactional
    public Asset update(Long id, Map<String, Object> body) {
        Asset s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        // SKU codes are immutable once issued (audit/inventory rows already
        // reference them). Update ignores any skuCode in the body.
        body.remove("skuCode");
        applyBody(s, body);
        return repo.save(s);
    }

    private void applyBody(Asset s, Map<String, Object> body) {
        if (body.containsKey("name")) s.setName((String) body.get("name"));
        if (body.containsKey("active")) s.setActive((Boolean) body.get("active"));
        if (body.containsKey("variant")) s.setVariant((String) body.get("variant"));
        if (body.containsKey("uomId")) {
            s.setUomId(body.get("uomId") != null
                    ? ((Number) body.get("uomId")).longValue() : null);
        }
        if (body.containsKey("productId")) {
            Long prodId = ((Number) body.get("productId")).longValue();
            s.setProduct(productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("AssetModel not found: " + prodId)));
        }
    }

    /**
     * Auto-build sku_code as "<BRAND_SLUG>-<VARIANT>" where:
     *   BRAND_SLUG = product.brand uppercased + non-alphanumerics → "-", trimmed.
     *                Falls back to product.code if brand is blank.
     *   VARIANT    = sku.variant uppercased + same slug rules. Falls back to "VAR".
     * If the resulting code collides with an existing SKU, append "-001", "-002"...
     */
    private String generateCode(Asset s) {
        AssetModel p = s.getProduct();
        String brandSource = (p != null && p.getBrand() != null && !p.getBrand().isBlank())
                ? p.getBrand()
                : (p != null ? p.getCode() : "SKU");
        String variantSource = (s.getVariant() != null && !s.getVariant().isBlank())
                ? s.getVariant()
                : "VAR";

        String base = slug(brandSource) + "-" + slug(variantSource);

        // Check collisions. If clean code is free, use it.
        if (repo.findBySkuCode(base).isEmpty()) {
            return base;
        }
        // Otherwise find the highest existing -NNN suffix and increment.
        String prefix = base + "-";
        int maxN = 0;
        for (Asset existing : repo.findBySkuCodeStartingWith(prefix)) {
            String tail = existing.getSkuCode().substring(prefix.length());
            try {
                int n = Integer.parseInt(tail);
                if (n > maxN) maxN = n;
            } catch (NumberFormatException ignored) {
                // Tail wasn't a pure number — ignore.
            }
        }
        return String.format("%s%03d", prefix, maxN + 1);
    }

    private String slug(String raw) {
        String s = raw.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return s.isEmpty() ? "SKU" : s;
    }

    @Transactional
    public void delete(Long id) {
        Asset s = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("SKU not found: " + id));
        repo.delete(s);
    }

    private Map<String, Object> toMap(Asset s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.getId());
        map.put("skuCode", s.getSkuCode());
        map.put("name", s.getName());
        map.put("variant", s.getVariant());
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