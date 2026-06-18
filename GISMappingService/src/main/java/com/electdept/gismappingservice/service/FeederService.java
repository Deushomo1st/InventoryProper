package com.electdept.gismappingservice.service;

import com.electdept.gismappingservice.model.Substation;
import com.electdept.gismappingservice.model.Feeder;
import com.electdept.gismappingservice.repository.PoleRepository;
import com.electdept.gismappingservice.repository.SubstationRepository;
import com.electdept.gismappingservice.repository.FeederRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class FeederService {

    private final FeederRepository repo;
    private final SubstationRepository warehouseRepo;
    private final PoleRepository binRepo;

    public FeederService(FeederRepository repo, SubstationRepository warehouseRepo, PoleRepository binRepo) {
        this.repo = repo;
        this.warehouseRepo = warehouseRepo;
        this.binRepo = binRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Feeder z : repo.findAll()) {
            result.add(toMap(z));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Feeder z = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Feeder not found: " + id));
        return toMap(z);
    }

    @Transactional
    public Feeder create(Map<String, Object> body) {
        Long warehouseId = body.get("warehouseId") != null ? ((Number) body.get("warehouseId")).longValue() : null;
        if (warehouseId == null) {
            throw new RuntimeException("Substation ID is required");
        }

        Substation warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Substation not found: " + warehouseId));

        String code = (String) body.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }

        if (repo.existsByWarehouseIdAndCode(warehouseId, code)) {
            throw new RuntimeException("Feeder code already exists in this warehouse: " + code);
        }

        Feeder z = new Feeder();
        z.setWarehouse(warehouse);
        applyBody(z, body);
        return repo.save(z);
    }

    @Transactional
    public Feeder update(Long id, Map<String, Object> body) {
        Feeder z = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Feeder not found: " + id));

        Long warehouseId = body.get("warehouseId") != null ? ((Number) body.get("warehouseId")).longValue() : null;
        if (warehouseId != null) {
            Substation warehouse = warehouseRepo.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("Substation not found: " + warehouseId));
            z.setWarehouse(warehouse);
        }

        String code = (String) body.get("code");
        Long whId = warehouseId != null ? warehouseId : z.getWarehouse().getId();
        if (code != null && !code.equals(z.getCode()) && repo.existsByWarehouseIdAndCode(whId, code)) {
            throw new RuntimeException("Feeder code already exists in this warehouse: " + code);
        }

        applyBody(z, body);
        return repo.save(z);
    }

    private void applyBody(Feeder z, Map<String, Object> body) {
        if (body.containsKey("code")) z.setCode((String) body.get("code"));
        if (body.containsKey("name")) z.setName((String) body.get("name"));
        if (body.containsKey("zoneType")) z.setZoneType((String) body.get("zoneType"));
        if (body.containsKey("active")) z.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Feeder z = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Feeder not found: " + id));

        // Cascade delete: first delete all bins in this zone
        binRepo.deleteAllByZoneId(id);

        // Then delete the zone
        repo.delete(z);
    }

    private Map<String, Object> toMap(Feeder z) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", z.getId());
        map.put("warehouseId", z.getWarehouse().getId());
        map.put("warehouseName", z.getWarehouse().getName());
        map.put("code", z.getCode());
        map.put("name", z.getName());
        map.put("zoneType", z.getZoneType());
        map.put("active", z.getActive());
        map.put("createdAt", z.getCreatedAt());
        return map;
    }
}
