package com.electdept.protectioncoordinationservice.service;

import com.electdept.protectioncoordinationservice.model.ProtectionScheme;
import com.electdept.protectioncoordinationservice.model.CoordinationStudy;
import com.electdept.protectioncoordinationservice.model.DeviceSelection;
import com.electdept.protectioncoordinationservice.repository.ProtectionSchemeRepository;
import com.electdept.protectioncoordinationservice.repository.DeviceSelectionRepository;
import com.electdept.protectioncoordinationservice.repository.CoordinationStudyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoordinationStudyService {
    private final CoordinationStudyRepository soRepo;
    private final DeviceSelectionRepository solRepo;
    private final ProtectionSchemeRepository customerRepo;

    public CoordinationStudyService(CoordinationStudyRepository soRepo, DeviceSelectionRepository solRepo, ProtectionSchemeRepository customerRepo) {
        this.soRepo = soRepo;
        this.solRepo = solRepo;
        this.customerRepo = customerRepo;
    }

    public List<Map<String, Object>> getAll() {
        return soRepo.findAll().stream()
                .map(this::toDtoWithLines)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        CoordinationStudy so = soRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CoordinationStudy not found: " + id));
        return toDtoWithLines(so);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Map<String, Object> body, Long currentUserId) {
        String code = findNextCode();
        
        CoordinationStudy so = new CoordinationStudy();
        so.setCode(code);
        so.setCustomerId(((Number) body.get("customerId")).longValue());
        so.setWarehouseId(((Number) body.get("warehouseId")).longValue());
        so.setStatus("DRAFT");
        so.setNotes((String) body.get("notes"));
        so.setCurrency((String) body.getOrDefault("currency", "USD"));
        so.setCreatedBy(currentUserId);
        so.setCreatedAt(Instant.now());
        
        CoordinationStudy saved = soRepo.save(so);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");
        if (lines != null && !lines.isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (Map<String, Object> lineBody : lines) {
                BigDecimal qty = new BigDecimal(lineBody.get("qty").toString());
                BigDecimal unitPrice = new BigDecimal(lineBody.get("unitPrice").toString());
                BigDecimal lineTotal = qty.multiply(unitPrice);
                
                DeviceSelection line = new DeviceSelection();
                line.setSalesOrderId(saved.getId());
                line.setSkuId(((Number) lineBody.get("skuId")).longValue());
                line.setQty(qty);
                line.setUnitPrice(unitPrice);
                line.setLineTotal(lineTotal);
                
                solRepo.save(line);
                totalAmount = totalAmount.add(lineTotal);
            }
            
            saved.setTotalAmount(totalAmount);
            soRepo.save(saved);
        }
        
        return toDtoWithLines(saved);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        CoordinationStudy so = soRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CoordinationStudy not found: " + id));
        
        if (!"DRAFT".equals(so.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be updated");
        }
        
        if (body.containsKey("notes")) so.setNotes((String) body.get("notes"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");
        if (lines != null) {
            solRepo.deleteBySalesOrderId(id);
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (Map<String, Object> lineBody : lines) {
                BigDecimal qty = new BigDecimal(lineBody.get("qty").toString());
                BigDecimal unitPrice = new BigDecimal(lineBody.get("unitPrice").toString());
                BigDecimal lineTotal = qty.multiply(unitPrice);
                
                DeviceSelection line = new DeviceSelection();
                line.setSalesOrderId(id);
                line.setSkuId(((Number) lineBody.get("skuId")).longValue());
                line.setQty(qty);
                line.setUnitPrice(unitPrice);
                line.setLineTotal(lineTotal);
                
                solRepo.save(line);
                totalAmount = totalAmount.add(lineTotal);
            }
            
            so.setTotalAmount(totalAmount);
        }
        
        return toDtoWithLines(soRepo.save(so));
    }

    @Transactional
    public void delete(Long id) {
        CoordinationStudy so = soRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CoordinationStudy not found: " + id));
        
        if (!"DRAFT".equals(so.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be deleted");
        }
        
        solRepo.deleteBySalesOrderId(id);
        soRepo.delete(so);
    }

    @Transactional
    public Map<String, Object> confirm(Long id) {
        CoordinationStudy so = soRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CoordinationStudy not found: " + id));
        
        if (!"DRAFT".equals(so.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be confirmed");
        }
        
        so.setStatus("CONFIRMED");
        so.setConfirmedAt(Instant.now());
        
        return toDtoWithLines(soRepo.save(so));
    }

    @Transactional
    public Map<String, Object> cancel(Long id, String reason) {
        CoordinationStudy so = soRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CoordinationStudy not found: " + id));
        
        if ("SHIPPED".equals(so.getStatus())) {
            throw new RuntimeException("Cannot cancel SHIPPED order");
        }
        
        so.setStatus("CANCELLED");
        so.setCancelledAt(Instant.now());
        
        return toDtoWithLines(soRepo.save(so));
    }

    private String findNextCode() {
        Optional<CoordinationStudy> last = soRepo.findTopByOrderByIdDesc();
        int next = 1;
        if (last.isPresent() && last.get().getCode() != null && last.get().getCode().startsWith("SO-")) {
            try {
                String numPart = last.get().getCode().substring(3);
                next = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException e) {
                // ignore, use default 1
            }
        }
        return String.format("SO-%04d", next);
    }

    private Map<String, Object> toDtoWithLines(CoordinationStudy so) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", so.getId());
        dto.put("code", so.getCode());
        dto.put("customerId", so.getCustomerId());
        dto.put("warehouseId", so.getWarehouseId());
        dto.put("status", so.getStatus());
        dto.put("notes", so.getNotes());
        dto.put("totalAmount", so.getTotalAmount());
        dto.put("currency", so.getCurrency());
        dto.put("createdBy", so.getCreatedBy());
        dto.put("createdAt", so.getCreatedAt());
        dto.put("confirmedAt", so.getConfirmedAt());
        dto.put("cancelledAt", so.getCancelledAt());
        
        // Denormalize customer name
        try {
            ProtectionScheme customer = customerRepo.findById(so.getCustomerId()).orElse(null);
            if (customer != null) {
                dto.put("customerName", customer.getName());
            }
        } catch (Exception e) {
            // ignore
        }
        
        // Include lines
        List<DeviceSelection> lines = solRepo.findBySalesOrderId(so.getId());
        dto.put("lineCount", lines.size());
        dto.put("lines", lines.stream().map(this::lineToDto).collect(Collectors.toList()));
        
        return dto;
    }

    private Map<String, Object> lineToDto(DeviceSelection line) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", line.getId());
        dto.put("salesOrderId", line.getSalesOrderId());
        dto.put("skuId", line.getSkuId());
        dto.put("qty", line.getQty());
        dto.put("unitPrice", line.getUnitPrice());
        dto.put("lineTotal", line.getLineTotal());
        return dto;
    }
}
