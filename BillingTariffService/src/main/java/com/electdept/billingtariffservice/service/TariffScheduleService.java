package com.electdept.billingtariffservice.service;

import com.electdept.billingtariffservice.model.TariffSchedule;
import com.electdept.billingtariffservice.model.TariffRate;
import com.electdept.billingtariffservice.repository.TariffRateRepository;
import com.electdept.billingtariffservice.repository.TariffScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TariffScheduleService {

    private final TariffScheduleRepository repo;
    private final TariffRateRepository entryRepo;

    public TariffScheduleService(TariffScheduleRepository repo, TariffRateRepository entryRepo) {
        this.repo = repo;
        this.entryRepo = entryRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<TariffSchedule> list = repo.findAll();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        TariffSchedule pl = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("TariffSchedule not found: " + id));
        Map<String, Object> dto = toDto(pl);
        
        List<Map<String, Object>> entries = entryRepo.findByPriceListId(id).stream()
                .map(e -> {
                    Map<String, Object> entryDto = new HashMap<>();
                    entryDto.put("id", e.getId());
                    entryDto.put("skuId", e.getSkuId());
                    entryDto.put("unitPrice", e.getUnitPrice());
                    return entryDto;
                })
                .collect(Collectors.toList());
        
        dto.put("entries", entries);
        return dto;
    }

    public Map<String, Object> create(Map<String, Object> body) {
        TariffSchedule pl = new TariffSchedule();
        pl.setCode((String) body.get("code"));
        pl.setName((String) body.get("name"));
        pl.setCustomerTier((String) body.get("customerTier"));
        pl.setCurrency(body.get("currency") != null ? (String) body.get("currency") : "USD");
        pl.setValidFrom(body.get("validFrom") != null ? java.time.LocalDate.parse((String) body.get("validFrom")) : null);
        pl.setValidTo(body.get("validTo") != null ? java.time.LocalDate.parse((String) body.get("validTo")) : null);
        pl.setActive(body.get("active") != null ? (Boolean) body.get("active") : true);
        
        pl = repo.save(pl);
        return toDto(pl);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        TariffSchedule pl = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("TariffSchedule not found: " + id));
        
        if (body.containsKey("code")) pl.setCode((String) body.get("code"));
        if (body.containsKey("name")) pl.setName((String) body.get("name"));
        if (body.containsKey("customerTier")) pl.setCustomerTier((String) body.get("customerTier"));
        if (body.containsKey("currency")) pl.setCurrency((String) body.get("currency"));
        if (body.containsKey("validFrom")) pl.setValidFrom(java.time.LocalDate.parse((String) body.get("validFrom")));
        if (body.containsKey("validTo")) pl.setValidTo(java.time.LocalDate.parse((String) body.get("validTo")));
        if (body.containsKey("active")) pl.setActive((Boolean) body.get("active"));
        
        pl = repo.save(pl);
        return toDto(pl);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public Map<String, Object> addEntry(Long priceListId, Map<String, Object> body) {
        TariffSchedule pl = repo.findById(priceListId)
                .orElseThrow(() -> new RuntimeException("TariffSchedule not found: " + priceListId));
        
        TariffRate entry = new TariffRate();
        entry.setPriceListId(priceListId);
        entry.setSkuId(((Number) body.get("skuId")).longValue());
        entry.setUnitPrice(new java.math.BigDecimal(body.get("unitPrice").toString()));
        
        entry = entryRepo.save(entry);
        return toEntryDto(entry);
    }

    public void deleteEntry(Long entryId) {
        entryRepo.deleteById(entryId);
    }

    private Map<String, Object> toDto(TariffSchedule pl) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", pl.getId());
        dto.put("code", pl.getCode());
        dto.put("name", pl.getName());
        dto.put("customerTier", pl.getCustomerTier());
        dto.put("currency", pl.getCurrency());
        dto.put("validFrom", pl.getValidFrom());
        dto.put("validTo", pl.getValidTo());
        dto.put("active", pl.getActive());
        dto.put("createdAt", pl.getCreatedAt());
        
        long entryCount = entryRepo.findByPriceListId(pl.getId()).size();
        dto.put("entryCount", entryCount);
        return dto;
    }

    private Map<String, Object> toEntryDto(TariffRate entry) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", entry.getId());
        dto.put("priceListId", entry.getPriceListId());
        dto.put("skuId", entry.getSkuId());
        dto.put("unitPrice", entry.getUnitPrice());
        return dto;
    }
}
