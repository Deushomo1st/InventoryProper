package com.electdept.outagemanagementservice.service;

import com.electdept.outagemanagementservice.client.InventoryClient;
import com.electdept.outagemanagementservice.model.OutageEvent;
import com.electdept.outagemanagementservice.model.AffectedAsset;
import com.electdept.outagemanagementservice.repository.AffectedAssetRepository;
import com.electdept.outagemanagementservice.repository.OutageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OutageEventService {
    private final OutageEventRepository shipmentRepo;
    private final AffectedAssetRepository shipmentLineRepo;
    private final InventoryClient inventoryClient;

    public OutageEventService(OutageEventRepository shipmentRepo, AffectedAssetRepository shipmentLineRepo, 
                          InventoryClient inventoryClient) {
        this.shipmentRepo = shipmentRepo;
        this.shipmentLineRepo = shipmentLineRepo;
        this.inventoryClient = inventoryClient;
    }

    public List<Map<String, Object>> getAll() {
        return shipmentRepo.findAll().stream()
                .map(this::toDtoWithLines)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        OutageEvent shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OutageEvent not found: " + id));
        return toDtoWithLines(shipment);
    }

    public List<Map<String, Object>> getBySalesOrderId(Long soId) {
        return shipmentRepo.findBySalesOrderId(soId).stream()
                .map(this::toDtoWithLines)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, Long currentUserId) {
        String code = findNextCode();
        
        OutageEvent shipment = new OutageEvent();
        shipment.setCode(code);
        shipment.setSalesOrderId(((Number) body.get("salesOrderId")).longValue());
        shipment.setWarehouseId(((Number) body.get("warehouseId")).longValue());
        shipment.setStatus("DRAFT");
        shipment.setNotes((String) body.get("notes"));
        shipment.setShippedBy(currentUserId);
        shipment.setCreatedAt(Instant.now());
        
        OutageEvent saved = shipmentRepo.save(shipment);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");
        if (lines != null && !lines.isEmpty()) {
            for (Map<String, Object> lineBody : lines) {
                AffectedAsset line = new AffectedAsset();
                line.setShipmentId(saved.getId());
                line.setSkuId(((Number) lineBody.get("skuId")).longValue());
                line.setBinId(((Number) lineBody.get("binId")).longValue());
                line.setQtyOrdered(new BigDecimal(lineBody.get("qtyOrdered").toString()));
                line.setQtyShipped(BigDecimal.ZERO);
                
                shipmentLineRepo.save(line);
            }
        }
        
        return toDtoWithLines(saved);
    }

    @Transactional
    public Map<String, Object> updateLineQty(Long lineId, BigDecimal qtyShipped, Long binId) {
        AffectedAsset line = shipmentLineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("AffectedAsset not found: " + lineId));
        
        OutageEvent shipment = shipmentRepo.findById(line.getShipmentId())
                .orElseThrow(() -> new RuntimeException("OutageEvent not found"));
        
        if (!"DRAFT".equals(shipment.getStatus())) {
            throw new RuntimeException("Only DRAFT shipments can be modified");
        }
        
        line.setQtyShipped(qtyShipped);
        if (binId != null) {
            line.setBinId(binId);
        }
        
        shipmentLineRepo.save(line);
        return toDtoWithLines(shipment);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> ship(Long id, String bearerToken) {
        OutageEvent shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OutageEvent not found: " + id));
        
        if (!"DRAFT".equals(shipment.getStatus())) {
            throw new RuntimeException("Only DRAFT shipments can be shipped");
        }
        
        List<AffectedAsset> lines = shipmentLineRepo.findByShipmentId(id);
        if (lines.isEmpty()) {
            throw new RuntimeException("Cannot ship without lines");
        }
        
        // Post negative adjustments to InventoryService for each line
        for (AffectedAsset line : lines) {
            if (line.getQtyShipped().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("All lines must have qty_shipped > 0");
            }
            
            // Negative delta because we're shipping out (reducing stock)
            BigDecimal delta = line.getQtyShipped().negate();
            inventoryClient.postAdjustment(
                bearerToken,
                line.getSkuId(),
                shipment.getWarehouseId(),
                line.getBinId(),
                delta,
                "ISSUE",
                "SO",
                shipment.getSalesOrderId()
            );
        }
        
        shipment.setStatus("SHIPPED");
        shipment.setShippedAt(Instant.now());
        
        return toDtoWithLines(shipmentRepo.save(shipment));
    }

    @Transactional
    public void delete(Long id) {
        OutageEvent shipment = shipmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("OutageEvent not found: " + id));
        
        if (!"DRAFT".equals(shipment.getStatus())) {
            throw new RuntimeException("Only DRAFT shipments can be deleted");
        }
        
        shipmentLineRepo.deleteByShipmentId(id);
        shipmentRepo.delete(shipment);
    }

    private String findNextCode() {
        Optional<OutageEvent> last = shipmentRepo.findTopByOrderByIdDesc();
        int next = 1;
        if (last.isPresent() && last.get().getCode() != null && last.get().getCode().startsWith("SHIP-")) {
            try {
                String numPart = last.get().getCode().substring(5);
                next = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException e) {
                // ignore, use default 1
            }
        }
        return String.format("SHIP-%04d", next);
    }

    private Map<String, Object> toDtoWithLines(OutageEvent shipment) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", shipment.getId());
        dto.put("code", shipment.getCode());
        dto.put("salesOrderId", shipment.getSalesOrderId());
        dto.put("warehouseId", shipment.getWarehouseId());
        dto.put("status", shipment.getStatus());
        dto.put("notes", shipment.getNotes());
        dto.put("carrier", shipment.getCarrier());
        dto.put("trackingNumber", shipment.getTrackingNumber());
        dto.put("shippedBy", shipment.getShippedBy());
        dto.put("createdAt", shipment.getCreatedAt());
        dto.put("shippedAt", shipment.getShippedAt());
        
        List<AffectedAsset> lines = shipmentLineRepo.findByShipmentId(shipment.getId());
        dto.put("lineCount", lines.size());
        dto.put("lines", lines.stream().map(this::lineToDto).collect(Collectors.toList()));
        
        return dto;
    }

    private Map<String, Object> lineToDto(AffectedAsset line) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", line.getId());
        dto.put("shipmentId", line.getShipmentId());
        dto.put("skuId", line.getSkuId());
        dto.put("binId", line.getBinId());
        dto.put("qtyOrdered", line.getQtyOrdered());
        dto.put("qtyShipped", line.getQtyShipped());
        return dto;
    }
}
