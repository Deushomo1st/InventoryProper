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
public class SubstationService {

    private final SubstationRepository repo;
    private final FeederRepository zoneRepo;
    private final PoleRepository binRepo;

    public SubstationService(SubstationRepository repo, FeederRepository zoneRepo, PoleRepository binRepo) {
        this.repo = repo;
        this.zoneRepo = zoneRepo;
        this.binRepo = binRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Substation w : repo.findAll()) {
            result.add(toMap(w));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Substation w = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Substation not found: " + id));
        return toMap(w);
    }

    @Transactional
    public Substation create(Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }
        if (repo.existsByCode(code)) {
            throw new RuntimeException("Substation code already exists: " + code);
        }

        Substation w = new Substation();
        applyBody(w, body);
        return repo.save(w);
    }

    @Transactional
    public Substation update(Long id, Map<String, Object> body) {
        Substation w = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Substation not found: " + id));

        String code = (String) body.get("code");
        if (code != null && !code.equals(w.getCode()) && repo.existsByCode(code)) {
            throw new RuntimeException("Substation code already exists: " + code);
        }

        applyBody(w, body);
        return repo.save(w);
    }

    private void applyBody(Substation w, Map<String, Object> body) {
        if (body.containsKey("code")) w.setCode((String) body.get("code"));
        if (body.containsKey("name")) w.setName((String) body.get("name"));
        if (body.containsKey("address")) w.setAddress((String) body.get("address"));
        if (body.containsKey("active")) w.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Substation w = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Substation not found: " + id));

        // Cascade delete: first delete all bins in zones of this warehouse
        List<Long> zoneIds = zoneRepo.findAll().stream()
                .filter(z -> z.getWarehouse().getId().equals(id))
                .map(Feeder::getId)
                .toList();

        for (Long zoneId : zoneIds) {
            binRepo.deleteAllByZoneId(zoneId);
        }

        // Then delete all zones
        zoneRepo.deleteAllByWarehouseId(id);

        // Finally delete the warehouse
        repo.delete(w);
    }

    private Map<String, Object> toMap(Substation w) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", w.getId());
        map.put("code", w.getCode());
        map.put("name", w.getName());
        map.put("address", w.getAddress());
        map.put("active", w.getActive());
        map.put("createdAt", w.getCreatedAt());
        return map;
    }
}
