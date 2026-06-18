package com.electdept.energymonitoringservice.service;

import com.electdept.energymonitoringservice.model.CurrentReading;
import com.electdept.energymonitoringservice.repository.CurrentReadingRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CurrentReadingService {

    private final CurrentReadingRepository repo;

    public CurrentReadingService(CurrentReadingRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CurrentReading sl : repo.findAll()) {
            result.add(toMap(sl));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        CurrentReading sl = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("CurrentReading not found: " + id));
        return toMap(sl);
    }

    private Map<String, Object> toMap(CurrentReading sl) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sl.getId());
        map.put("skuId", sl.getSkuId());
        map.put("warehouseId", sl.getWarehouseId());
        map.put("binId", sl.getBinId());
        map.put("qtyOnHand", sl.getQtyOnHand());
        map.put("lastUpdated", sl.getLastUpdated());
        return map;
    }
}
