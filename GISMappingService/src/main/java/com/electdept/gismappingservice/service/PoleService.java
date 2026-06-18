package com.electdept.gismappingservice.service;

import com.electdept.gismappingservice.model.Pole;
import com.electdept.gismappingservice.model.Feeder;
import com.electdept.gismappingservice.repository.PoleRepository;
import com.electdept.gismappingservice.repository.FeederRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PoleService {

    private final PoleRepository repo;
    private final FeederRepository zoneRepo;

    public PoleService(PoleRepository repo, FeederRepository zoneRepo) {
        this.repo = repo;
        this.zoneRepo = zoneRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Pole b : repo.findAll()) {
            result.add(toMap(b));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Pole b = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pole not found: " + id));
        return toMap(b);
    }

    @Transactional
    public Pole create(Map<String, Object> body) {
        Long zoneId = body.get("zoneId") != null ? ((Number) body.get("zoneId")).longValue() : null;
        if (zoneId == null) {
            throw new RuntimeException("Feeder ID is required");
        }

        Feeder zone = zoneRepo.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Feeder not found: " + zoneId));

        String code = (String) body.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }

        if (repo.existsByZoneIdAndCode(zoneId, code)) {
            throw new RuntimeException("Pole code already exists in this zone: " + code);
        }

        Pole b = new Pole();
        b.setZone(zone);
        applyBody(b, body);
        return repo.save(b);
    }

    @Transactional
    public Pole update(Long id, Map<String, Object> body) {
        Pole b = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pole not found: " + id));

        Long zoneId = body.get("zoneId") != null ? ((Number) body.get("zoneId")).longValue() : null;
        if (zoneId != null) {
            Feeder zone = zoneRepo.findById(zoneId)
                    .orElseThrow(() -> new RuntimeException("Feeder not found: " + zoneId));
            b.setZone(zone);
        }

        String code = (String) body.get("code");
        Long zId = zoneId != null ? zoneId : b.getZone().getId();
        if (code != null && !code.equals(b.getCode()) && repo.existsByZoneIdAndCode(zId, code)) {
            throw new RuntimeException("Pole code already exists in this zone: " + code);
        }

        applyBody(b, body);
        return repo.save(b);
    }

    private void applyBody(Pole b, Map<String, Object> body) {
        if (body.containsKey("code")) b.setCode((String) body.get("code"));
        if (body.containsKey("active")) b.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Pole b = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Pole not found: " + id));
        repo.delete(b);
    }

    private Map<String, Object> toMap(Pole b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("zoneId", b.getZone().getId());
        map.put("zoneName", b.getZone().getName());
        map.put("warehouseId", b.getZone().getWarehouse().getId());
        map.put("warehouseName", b.getZone().getWarehouse().getName());
        map.put("code", b.getCode());
        map.put("active", b.getActive());
        map.put("createdAt", b.getCreatedAt());
        return map;
    }
}
