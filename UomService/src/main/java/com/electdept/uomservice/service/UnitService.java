package com.electdept.uomservice.service;

import com.electdept.uomservice.model.Unit;
import com.electdept.uomservice.repository.ConversionRepository;
import com.electdept.uomservice.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UnitService {

    private final UnitRepository repo;
    private final ConversionRepository conversionRepo;

    public UnitService(UnitRepository repo, ConversionRepository conversionRepo) {
        this.repo = repo;
        this.conversionRepo = conversionRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Unit u : repo.findAll()) {
            result.add(toMap(u));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Unit u = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + id));
        return toMap(u);
    }

    @Transactional
    public Unit create(Map<String, Object> body) {
        String code = (String) body.get("code");
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }
        if (repo.existsByCode(code)) {
            throw new RuntimeException("Unit code already exists: " + code);
        }

        Unit u = new Unit();
        applyBody(u, body);
        return repo.save(u);
    }

    @Transactional
    public Unit update(Long id, Map<String, Object> body) {
        Unit u = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + id));

        String code = (String) body.get("code");
        if (code != null && !code.equals(u.getCode()) && repo.existsByCode(code)) {
            throw new RuntimeException("Unit code already exists: " + code);
        }

        applyBody(u, body);
        return repo.save(u);
    }

    private void applyBody(Unit u, Map<String, Object> body) {
        if (body.containsKey("code")) u.setCode((String) body.get("code"));
        if (body.containsKey("name")) u.setName((String) body.get("name"));
        if (body.containsKey("active")) u.setActive((Boolean) body.get("active"));
    }

    @Transactional
    public void delete(Long id) {
        Unit u = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + id));

        // Cascade-clean conversions that reference this unit
        conversionRepo.deleteAllByFromUnitId(id);
        conversionRepo.deleteAllByToUnitId(id);

        repo.delete(u);
    }

    private Map<String, Object> toMap(Unit u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("code", u.getCode());
        map.put("name", u.getName());
        map.put("active", u.getActive());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }
}
