package com.electdept.assetmanagementservice.service;

import com.electdept.assetmanagementservice.model.AssetSpec;
import com.electdept.assetmanagementservice.repository.AssetSpecRepository;
import com.electdept.assetmanagementservice.repository.AssetModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AssetSpecService {

    private final AssetSpecRepository repo;
    private final AssetModelRepository productRepo;

    public AssetSpecService(AssetSpecRepository repo, AssetModelRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetSpec a : repo.findAll()) {
            result.add(toMap(a));
        }
        return result;
    }

    public List<Map<String, Object>> getByProductId(Long productId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AssetSpec a : repo.findByProductId(productId)) {
            result.add(toMap(a));
        }
        return result;
    }

    @Transactional
    public AssetSpec create(Map<String, Object> body) {
        AssetSpec a = new AssetSpec();
        applyBody(a, body);
        return repo.save(a);
    }

    @Transactional
    public AssetSpec update(Long id, Map<String, Object> body) {
        AssetSpec a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        applyBody(a, body);
        return repo.save(a);
    }

    private void applyBody(AssetSpec a, Map<String, Object> body) {
        if (body.containsKey("key")) a.setKey((String) body.get("key"));
        if (body.containsKey("value")) a.setValue((String) body.get("value"));
        if (body.containsKey("productId")) {
            Long prodId = ((Number) body.get("productId")).longValue();
            a.setProduct(productRepo.findById(prodId)
                    .orElseThrow(() -> new RuntimeException("AssetModel not found: " + prodId)));
        }
    }

    @Transactional
    public void delete(Long id) {
        AssetSpec a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Attribute not found: " + id));
        repo.delete(a);
    }

    private Map<String, Object> toMap(AssetSpec a) {
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