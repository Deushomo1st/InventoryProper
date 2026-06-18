package com.electdept.uomservice.service;

import com.electdept.uomservice.model.Conversion;
import com.electdept.uomservice.model.Unit;
import com.electdept.uomservice.repository.ConversionRepository;
import com.electdept.uomservice.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ConversionService {

    private final ConversionRepository repo;
    private final UnitRepository unitRepo;

    public ConversionService(ConversionRepository repo, UnitRepository unitRepo) {
        this.repo = repo;
        this.unitRepo = unitRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversion c : repo.findAll()) {
            result.add(toMap(c));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        Conversion c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversion not found: " + id));
        return toMap(c);
    }

    @Transactional
    public Conversion create(Map<String, Object> body) {
        Long fromUnitId = body.get("fromUnitId") != null ? ((Number) body.get("fromUnitId")).longValue() : null;
        Long toUnitId = body.get("toUnitId") != null ? ((Number) body.get("toUnitId")).longValue() : null;

        if (fromUnitId == null || toUnitId == null) {
            throw new RuntimeException("Both fromUnitId and toUnitId are required");
        }

        if (fromUnitId.equals(toUnitId)) {
            throw new RuntimeException("Cannot convert a unit to itself");
        }

        Optional<Conversion> existing = repo.findByFromUnitIdAndToUnitId(fromUnitId, toUnitId);
        if (existing.isPresent()) {
            throw new RuntimeException("Conversion already exists between these units");
        }

        Unit fromUnit = unitRepo.findById(fromUnitId)
                .orElseThrow(() -> new RuntimeException("From unit not found: " + fromUnitId));
        Unit toUnit = unitRepo.findById(toUnitId)
                .orElseThrow(() -> new RuntimeException("To unit not found: " + toUnitId));

        Conversion c = new Conversion();
        c.setFromUnit(fromUnit);
        c.setToUnit(toUnit);

        Object factorObj = body.get("factor");
        if (factorObj == null) {
            throw new RuntimeException("Factor is required");
        }
        c.setFactor(new BigDecimal(factorObj.toString()));

        return repo.save(c);
    }

    @Transactional
    public Conversion update(Long id, Map<String, Object> body) {
        Conversion c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversion not found: " + id));

        Long fromUnitId = body.get("fromUnitId") != null ? ((Number) body.get("fromUnitId")).longValue() : null;
        Long toUnitId = body.get("toUnitId") != null ? ((Number) body.get("toUnitId")).longValue() : null;

        if (fromUnitId != null && toUnitId != null) {
            if (fromUnitId.equals(toUnitId)) {
                throw new RuntimeException("Cannot convert a unit to itself");
            }

            Optional<Conversion> existing = repo.findByFromUnitIdAndToUnitId(fromUnitId, toUnitId);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new RuntimeException("Conversion already exists between these units");
            }

            Unit fromUnit = unitRepo.findById(fromUnitId)
                    .orElseThrow(() -> new RuntimeException("From unit not found: " + fromUnitId));
            Unit toUnit = unitRepo.findById(toUnitId)
                    .orElseThrow(() -> new RuntimeException("To unit not found: " + toUnitId));
            c.setFromUnit(fromUnit);
            c.setToUnit(toUnit);
        }

        if (body.containsKey("factor")) {
            c.setFactor(new BigDecimal(body.get("factor").toString()));
        }

        return repo.save(c);
    }

    @Transactional
    public void delete(Long id) {
        Conversion c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversion not found: " + id));
        repo.delete(c);
    }

    private Map<String, Object> toMap(Conversion c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("fromUnitId", c.getFromUnit().getId());
        map.put("fromUnitCode", c.getFromUnit().getCode());
        map.put("fromUnitName", c.getFromUnit().getName());
        map.put("toUnitId", c.getToUnit().getId());
        map.put("toUnitCode", c.getToUnit().getCode());
        map.put("toUnitName", c.getToUnit().getName());
        map.put("factor", c.getFactor());
        map.put("createdAt", c.getCreatedAt());
        return map;
    }
}
