package com.electdept.assetmanagementservice.service;

import com.electdept.assetmanagementservice.model.AssetModel;
import com.electdept.assetmanagementservice.model.AssetSpec;
import com.electdept.assetmanagementservice.model.Asset;
import com.electdept.assetmanagementservice.repository.AssetClassRepository;
import com.electdept.assetmanagementservice.repository.AssetSpecRepository;
import com.electdept.assetmanagementservice.repository.AssetModelRepository;
import com.electdept.assetmanagementservice.repository.AssetRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssetModelService {

    private final AssetModelRepository repo;
    private final AssetClassRepository categoryRepo;
    private final AssetRepository skuRepo;
    private final AssetSpecRepository attrRepo;

    public AssetModelService(AssetModelRepository repo,
                          AssetClassRepository categoryRepo,
                          AssetRepository skuRepo,
                          AssetSpecRepository attrRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.skuRepo = skuRepo;
        this.attrRepo = attrRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetModel p : repo.findAll()) {
            result.add(toMap(p));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        AssetModel p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("AssetModel not found: " + id));
        return toMap(p);
    }

    @Transactional
    public AssetModel create(Map<String, Object> body) {
        AssetModel p = new AssetModel();
        applyBody(p, body);
        // Always auto-generate the code on create. Even if the request body
        // smuggled a `code` field, we ignore it — there's no manual entry on
        // the create UI. This guarantees serial codes never collide with
        // hand-typed ones.
        p.setCode(nextSerialCode(0));
        return repo.save(p);
    }

    @Transactional
    public List<AssetModel> createBulk(Map<String, Object> body, int quantity) {
        if (quantity < 1) quantity = 1;
        if (quantity > 100) quantity = 100;     // sanity cap

        int start = findMaxSerial();
        List<AssetModel> products = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            AssetModel p = new AssetModel();
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
        for (AssetModel p : repo.findAll()) {
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
    public AssetModel update(Long id, Map<String, Object> body) {
        AssetModel p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("AssetModel not found: " + id));
        applyBody(p, body);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    private void applyBody(AssetModel p, Map<String, Object> body) {
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
                        .orElseThrow(() -> new RuntimeException("AssetClass not found: " + catId)));
            } else {
                p.setCategory(null);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        AssetModel p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("AssetModel not found: " + id));

        // Cascade-delete the product's own children first (attributes + SKUs).
        // If any SKU has operational references (stock_level, pricelist, PO/SO
        // lines, etc.) the SKU delete will fail with a different FK constraint
        // — at that point the product genuinely shouldn't be deleted (would
        // orphan history). We catch and surface a friendly suggestion to
        // discontinue the product instead by editing its status.
        try {
            for (AssetSpec a : attrRepo.findByProductId(id)) {
                attrRepo.delete(a);
            }
            // Force the attribute deletes to flush before we try the SKUs,
            // otherwise Hibernate batches everything to transaction commit
            // and the FK exception escapes past our catch block.
            attrRepo.flush();

            for (Asset s : skuRepo.findByProductId(id)) {
                skuRepo.delete(s);
            }
            skuRepo.flush();

            repo.delete(p);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException(
                "AssetModel '" + p.getName() + "' has SKUs with active inventory, " +
                "pricelist entries, or order history. Deactivate it instead — " +
                "edit the product and uncheck the Active checkbox to hide it from " +
                "listings without breaking history."
            );
        }
    }

    private Map<String, Object> toMap(AssetModel p) {
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