package com.electdept.cablesizingservice.service;

import com.electdept.cablesizingservice.client.InventoryClient;
import com.electdept.cablesizingservice.client.ProcurementClient;
import com.electdept.cablesizingservice.model.CableSizingRequest;
import com.electdept.cablesizingservice.model.CableSizingResult;
import com.electdept.cablesizingservice.repository.CableSizingRequestRepository;
import com.electdept.cablesizingservice.repository.CableSizingResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CableSizingResultService {

    private final CableSizingResultRepository suggestionRepo;
    private final CableSizingRequestRepository ruleRepo;
    private final InventoryClient inventoryClient;
    private final ProcurementClient procurementClient;

    public CableSizingResultService(CableSizingResultRepository suggestionRepo, CableSizingRequestRepository ruleRepo,
                             InventoryClient inventoryClient, ProcurementClient procurementClient) {
        this.suggestionRepo = suggestionRepo;
        this.ruleRepo = ruleRepo;
        this.inventoryClient = inventoryClient;
        this.procurementClient = procurementClient;
    }

    public List<Map<String, Object>> getAll(String status) {
        List<CableSizingResult> suggestions;
        if (status != null) {
            suggestions = suggestionRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            suggestions = suggestionRepo.findAll();
        }
        return suggestions.stream().map(this::toDto).toList();
    }

    public Map<String, Object> getById(Long id) {
        CableSizingResult suggestion = suggestionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CableSizingResult not found: " + id));
        return toDto(suggestion);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runSync(String bearerToken) {
        // Step 1: Fetch all levels from InventoryService
        List<Map<String, Object>> levels = inventoryClient.listLevels(bearerToken);
        
        // Step 2: Aggregate qtyOnHand by (skuId, warehouseId)
        Map<String, BigDecimal> aggregated = levels.stream()
                .collect(Collectors.groupingBy(
                        level -> level.get("skuId") + "_" + level.get("warehouseId"),
                        Collectors.reducing(BigDecimal.ZERO,
                                level -> new BigDecimal(level.get("qtyOnHand").toString()),
                                BigDecimal::add)
                ));
        
        // Step 3: Get all active rules
        List<CableSizingRequest> activeRules = ruleRepo.findByActiveTrue();
        
        int suggestionsCreated = 0;
        int suggestionsExisting = 0;
        
        // Step 4: For each rule, check if current qty < reorder point
        for (CableSizingRequest rule : activeRules) {
            String key = rule.getSkuId() + "_" + rule.getWarehouseId();
            BigDecimal currentQty = aggregated.getOrDefault(key, BigDecimal.ZERO);

            List<CableSizingResult> existingPending = suggestionRepo.findByRuleIdAndStatus(rule.getId(), "PENDING");

            if (currentQty.compareTo(rule.getReorderPoint()) < 0 && existingPending.isEmpty()) {
                // Create new suggestion
                CableSizingResult suggestion = new CableSizingResult();
                suggestion.setRuleId(rule.getId());
                suggestion.setSkuId(rule.getSkuId());
                suggestion.setWarehouseId(rule.getWarehouseId());
                suggestion.setCurrentQty(currentQty);
                suggestion.setSuggestedQty(rule.getReorderQty());
                
                suggestionRepo.save(suggestion);
                suggestionsCreated++;
            } else if (currentQty.compareTo(rule.getReorderPoint()) < 0 && !existingPending.isEmpty()) {
                suggestionsExisting++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("rulesChecked", activeRules.size());
        result.put("suggestionsCreated", suggestionsCreated);
        result.put("suggestionsExisting", suggestionsExisting);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> order(Long id, Long userId, String bearerToken) {
        CableSizingResult suggestion = suggestionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CableSizingResult not found: " + id));

        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new RuntimeException("Only PENDING suggestions can be ordered");
        }

        // Look up the rule
        CableSizingRequest rule = ruleRepo.findById(suggestion.getRuleId())
                .orElseThrow(() -> new RuntimeException("CableSizingRequest not found: " + suggestion.getRuleId()));
        
        // Call ProcurementService to create draft PO
        Map<String, Object> poResponse = procurementClient.createDraftOrder(
                bearerToken,
                rule.getPreferredSupplierId(),
                suggestion.getWarehouseId(),
                suggestion.getSkuId(),
                suggestion.getSuggestedQty(),
                rule.getDefaultUnitPrice()
        );
        
        // Extract PO id from response
        Long poId = ((Number) poResponse.get("id")).longValue();
        
        // Update suggestion
        suggestion.setStatus("ORDERED");
        suggestion.setCreatedPoId(poId);
        suggestion.setOrderedAt(Instant.now());
        suggestion.setCreatedBy(userId);
        
        suggestionRepo.save(suggestion);
        
        return toDto(suggestion);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dismiss(Long id) {
        CableSizingResult suggestion = suggestionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CableSizingResult not found: " + id));
        
        if (!"PENDING".equals(suggestion.getStatus())) {
            throw new RuntimeException("Only PENDING suggestions can be dismissed");
        }
        
        suggestion.setStatus("DISMISSED");
        suggestion.setDismissedAt(Instant.now());
        
        suggestionRepo.save(suggestion);
        
        return toDto(suggestion);
    }

    private Map<String, Object> toDto(CableSizingResult suggestion) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", suggestion.getId());
        map.put("ruleId", suggestion.getRuleId());
        map.put("skuId", suggestion.getSkuId());
        map.put("warehouseId", suggestion.getWarehouseId());
        map.put("currentQty", suggestion.getCurrentQty());
        map.put("suggestedQty", suggestion.getSuggestedQty());
        map.put("status", suggestion.getStatus());
        map.put("createdPoId", suggestion.getCreatedPoId());
        map.put("createdBy", suggestion.getCreatedBy());
        map.put("createdAt", suggestion.getCreatedAt());
        map.put("orderedAt", suggestion.getOrderedAt());
        map.put("dismissedAt", suggestion.getDismissedAt());
        return map;
    }
}
