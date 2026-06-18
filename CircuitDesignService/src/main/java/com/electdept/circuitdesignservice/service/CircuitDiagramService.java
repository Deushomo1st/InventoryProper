package com.electdept.circuitdesignservice.service;

import com.electdept.circuitdesignservice.client.InventoryClient;
import com.electdept.circuitdesignservice.model.CircuitDiagram;
import com.electdept.circuitdesignservice.model.DiagramElement;
import com.electdept.circuitdesignservice.repository.DiagramElementRepository;
import com.electdept.circuitdesignservice.repository.CircuitDiagramRepository;
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
public class CircuitDiagramService {
    private final CircuitDiagramRepository movementRepo;
    private final DiagramElementRepository movementLineRepo;
    private final InventoryClient inventoryClient;

    public CircuitDiagramService(CircuitDiagramRepository movementRepo, DiagramElementRepository movementLineRepo, 
                          InventoryClient inventoryClient) {
        this.movementRepo = movementRepo;
        this.movementLineRepo = movementLineRepo;
        this.inventoryClient = inventoryClient;
    }

    public List<Map<String, Object>> getAll() {
        return movementRepo.findAll().stream()
                .map(this::toDtoWithLines)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        CircuitDiagram movement = movementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CircuitDiagram not found: " + id));
        return toDtoWithLines(movement);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, Long currentUserId) {
        String code = findNextCode();
        
        CircuitDiagram movement = new CircuitDiagram();
        movement.setCode(code);
        movement.setWarehouseId(((Number) body.get("warehouseId")).longValue());
        movement.setStatus("DRAFT");
        movement.setNotes((String) body.get("notes"));
        movement.setReason((String) body.get("reason"));
        movement.setExecutedBy(currentUserId);
        movement.setCreatedAt(Instant.now());
        
        CircuitDiagram saved = movementRepo.save(movement);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) body.get("lines");
        if (lines != null && !lines.isEmpty()) {
            for (Map<String, Object> lineBody : lines) {
                Long sourceBinId = ((Number) lineBody.get("sourceBinId")).longValue();
                Long destinationBinId = ((Number) lineBody.get("destinationBinId")).longValue();
                
                if (sourceBinId.equals(destinationBinId)) {
                    throw new RuntimeException("Source bin and destination bin must be different");
                }
                
                DiagramElement line = new DiagramElement();
                line.setMovementId(saved.getId());
                line.setSkuId(((Number) lineBody.get("skuId")).longValue());
                line.setSourceBinId(sourceBinId);
                line.setDestinationBinId(destinationBinId);
                line.setQty(new BigDecimal(lineBody.get("qty").toString()));
                
                movementLineRepo.save(line);
            }
        }
        
        return toDtoWithLines(saved);
    }

    @Transactional
    public Map<String, Object> updateLine(Long lineId, Map<String, Object> body) {
        DiagramElement line = movementLineRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("DiagramElement not found: " + lineId));
        
        CircuitDiagram movement = movementRepo.findById(line.getMovementId())
                .orElseThrow(() -> new RuntimeException("CircuitDiagram not found"));
        
        if (!"DRAFT".equals(movement.getStatus())) {
            throw new RuntimeException("Only DRAFT movements can be modified");
        }
        
        if (body.containsKey("qty")) {
            line.setQty(new BigDecimal(body.get("qty").toString()));
        }
        if (body.containsKey("sourceBinId")) {
            line.setSourceBinId(((Number) body.get("sourceBinId")).longValue());
        }
        if (body.containsKey("destinationBinId")) {
            line.setDestinationBinId(((Number) body.get("destinationBinId")).longValue());
        }
        
        // Validate source != destination after update
        if (line.getSourceBinId().equals(line.getDestinationBinId())) {
            throw new RuntimeException("Source bin and destination bin must be different");
        }
        
        movementLineRepo.save(line);
        return toDtoWithLines(movement);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long id, String bearerToken) {
        CircuitDiagram movement = movementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CircuitDiagram not found: " + id));
        
        if (!"DRAFT".equals(movement.getStatus())) {
            throw new RuntimeException("Only DRAFT movements can be executed");
        }
        
        List<DiagramElement> lines = movementLineRepo.findByMovementId(id);
        if (lines.isEmpty()) {
            throw new RuntimeException("Cannot execute without lines");
        }
        
        // Post paired adjustments to InventoryService for each line
        for (DiagramElement line : lines) {
            if (line.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("All lines must have qty > 0");
            }
            
            // Source bin: negative delta (removing stock)
            inventoryClient.postAdjustment(
                bearerToken,
                line.getSkuId(),
                movement.getWarehouseId(),
                line.getSourceBinId(),
                line.getQty().negate(),
                "MOVE",
                "MV",
                movement.getId()
            );
            
            // Destination bin: positive delta (adding stock)
            inventoryClient.postAdjustment(
                bearerToken,
                line.getSkuId(),
                movement.getWarehouseId(),
                line.getDestinationBinId(),
                line.getQty(),
                "MOVE",
                "MV",
                movement.getId()
            );
        }
        
        movement.setStatus("EXECUTED");
        movement.setExecutedAt(Instant.now());
        
        return toDtoWithLines(movementRepo.save(movement));
    }

    @Transactional
    public void delete(Long id) {
        CircuitDiagram movement = movementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("CircuitDiagram not found: " + id));
        
        if (!"DRAFT".equals(movement.getStatus())) {
            throw new RuntimeException("Only DRAFT movements can be deleted");
        }
        
        movementLineRepo.deleteByMovementId(id);
        movementRepo.delete(movement);
    }

    private String findNextCode() {
        Optional<CircuitDiagram> last = movementRepo.findTopByOrderByIdDesc();
        int next = 1;
        if (last.isPresent() && last.get().getCode() != null && last.get().getCode().startsWith("MV-")) {
            try {
                String numPart = last.get().getCode().substring(3);
                next = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException e) {
                // ignore, use default 1
            }
        }
        return String.format("MV-%04d", next);
    }

    private Map<String, Object> toDtoWithLines(CircuitDiagram movement) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", movement.getId());
        dto.put("code", movement.getCode());
        dto.put("warehouseId", movement.getWarehouseId());
        dto.put("status", movement.getStatus());
        dto.put("notes", movement.getNotes());
        dto.put("reason", movement.getReason());
        dto.put("executedBy", movement.getExecutedBy());
        dto.put("createdAt", movement.getCreatedAt());
        dto.put("executedAt", movement.getExecutedAt());
        
        List<DiagramElement> lines = movementLineRepo.findByMovementId(movement.getId());
        dto.put("lineCount", lines.size());
        dto.put("lines", lines.stream().map(this::lineToDto).collect(Collectors.toList()));
        
        return dto;
    }

    private Map<String, Object> lineToDto(DiagramElement line) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", line.getId());
        dto.put("movementId", line.getMovementId());
        dto.put("skuId", line.getSkuId());
        dto.put("sourceBinId", line.getSourceBinId());
        dto.put("destinationBinId", line.getDestinationBinId());
        dto.put("qty", line.getQty());
        return dto;
    }
}
