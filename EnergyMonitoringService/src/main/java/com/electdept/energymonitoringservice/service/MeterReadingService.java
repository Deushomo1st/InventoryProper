package com.electdept.energymonitoringservice.service;

import com.electdept.energymonitoringservice.model.MeterReading;
import com.electdept.energymonitoringservice.model.CurrentReading;
import com.electdept.energymonitoringservice.repository.MeterReadingRepository;
import com.electdept.energymonitoringservice.repository.CurrentReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MeterReadingService {

    private final MeterReadingRepository ledgerRepo;
    private final CurrentReadingRepository levelRepo;

    public MeterReadingService(MeterReadingRepository ledgerRepo, CurrentReadingRepository levelRepo) {
        this.ledgerRepo = ledgerRepo;
        this.levelRepo = levelRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<MeterReading> ledgers = ledgerRepo.findAll();
        ledgers.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeterReading sl : ledgers) {
            result.add(toMap(sl));
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        MeterReading sl = ledgerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("MeterReading not found: " + id));
        return toMap(sl);
    }

    @Transactional
    public MeterReading recordAdjustment(Map<String, Object> body, Long callerUserId) {
        Long skuId = body.get("skuId") != null ? ((Number) body.get("skuId")).longValue() : null;
        Long warehouseId = body.get("warehouseId") != null ? ((Number) body.get("warehouseId")).longValue() : null;
        Long binId = body.get("binId") != null ? ((Number) body.get("binId")).longValue() : null;
        BigDecimal delta = body.get("delta") != null ? new BigDecimal(body.get("delta").toString()) : BigDecimal.ZERO;
        String reasonCode = (String) body.get("reasonCode");
        String refDocType = (String) body.get("refDocType");
        Long refDocId = body.get("refDocId") != null ? ((Number) body.get("refDocId")).longValue() : null;

        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("Delta must not be zero");
        }
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new RuntimeException("Reason code is required");
        }

        MeterReading ledger = new MeterReading();
        ledger.setSkuId(skuId);
        ledger.setWarehouseId(warehouseId);
        ledger.setBinId(binId);
        ledger.setDelta(delta);
        ledger.setReasonCode(reasonCode);
        ledger.setRefDocType(refDocType);
        ledger.setRefDocId(refDocId);
        ledger.setUserId(callerUserId);
        ledgerRepo.save(ledger);

        CurrentReading level = levelRepo.findBySkuIdAndWarehouseIdAndBinId(skuId, warehouseId, binId)
                .orElseGet(() -> {
                    CurrentReading newLevel = new CurrentReading();
                    newLevel.setSkuId(skuId);
                    newLevel.setWarehouseId(warehouseId);
                    newLevel.setBinId(binId);
                    newLevel.setQtyOnHand(BigDecimal.ZERO);
                    return newLevel;
                });

        level.setQtyOnHand(level.getQtyOnHand().add(delta));
        level.setLastUpdated(LocalDateTime.now());
        levelRepo.save(level);

        return ledger;
    }

    private Map<String, Object> toMap(MeterReading sl) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sl.getId());
        map.put("skuId", sl.getSkuId());
        map.put("warehouseId", sl.getWarehouseId());
        map.put("binId", sl.getBinId());
        map.put("delta", sl.getDelta());
        map.put("reasonCode", sl.getReasonCode());
        map.put("refDocType", sl.getRefDocType());
        map.put("refDocId", sl.getRefDocId());
        map.put("userId", sl.getUserId());
        map.put("createdAt", sl.getCreatedAt());
        return map;
    }
}
