package com.electdept.scadaintegrationservice.service;

import com.electdept.scadaintegrationservice.client.InventoryClient;
import com.electdept.scadaintegrationservice.model.TelemetryBatch;
import com.electdept.scadaintegrationservice.model.TelemetryReading;
import com.electdept.scadaintegrationservice.repository.TelemetryReadingRepository;
import com.electdept.scadaintegrationservice.repository.TelemetryBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TelemetryBatchService {

    private final TelemetryBatchRepository grRepo;
    private final TelemetryReadingRepository grlRepo;
    private final InventoryClient inventoryClient;

    public TelemetryBatchService(TelemetryBatchRepository grRepo, TelemetryReadingRepository grlRepo,
                               InventoryClient inventoryClient) {
        this.grRepo = grRepo;
        this.grlRepo = grlRepo;
        this.inventoryClient = inventoryClient;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TelemetryBatch gr : grRepo.findAll()) {
            Map<String, Object> map = toMap(gr);
            long lineCount = grlRepo.findByGoodsReceiptId(gr.getId()).size();
            map.put("lineCount", lineCount);
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        TelemetryBatch gr = grRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("TelemetryBatch not found: " + id));
        Map<String, Object> map = toMap(gr);
        List<TelemetryReading> lines = grlRepo.findByGoodsReceiptId(id);
        map.put("lines", lines.stream().map(this::lineToMap).collect(Collectors.toList()));
        return map;
    }

    @Transactional
    public TelemetryBatch create(Map<String, Object> body, Long currentUserId) {
        String code = findNextCode();
        Long purchaseOrderId = ((Number) body.get("purchaseOrderId")).longValue();
        Long warehouseId = ((Number) body.get("warehouseId")).longValue();
        String notes = (String) body.get("notes");

        TelemetryBatch gr = new TelemetryBatch();
        gr.setCode(code);
        gr.setPurchaseOrderId(purchaseOrderId);
        gr.setWarehouseId(warehouseId);
        gr.setStatus("DRAFT");
        gr.setNotes(notes);
        gr.setReceivedBy(currentUserId);
        grRepo.save(gr);

        List<Map<String, Object>> lineBodies = (List<Map<String, Object>>) body.get("lines");
        if (lineBodies != null) {
            for (Map<String, Object> lb : lineBodies) {
                Long skuId = ((Number) lb.get("skuId")).longValue();
                BigDecimal qtyOrdered = new BigDecimal(lb.get("qtyOrdered").toString());
                Long binId = ((Number) lb.get("binId")).longValue();

                TelemetryReading line = new TelemetryReading();
                line.setGoodsReceiptId(gr.getId());
                line.setSkuId(skuId);
                line.setQtyOrdered(qtyOrdered);
                line.setQtyReceived(BigDecimal.ZERO);
                line.setBinId(binId);
                grlRepo.save(line);
            }
        }

        return gr;
    }

    @Transactional
    public TelemetryBatch update(Long id, Map<String, Object> body) {
        TelemetryBatch gr = grRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("TelemetryBatch not found: " + id));
        if (!"DRAFT".equals(gr.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be updated");
        }

        Long warehouseId = body.get("warehouseId") != null ? ((Number) body.get("warehouseId")).longValue() : null;
        String notes = (String) body.get("notes");

        if (warehouseId != null) gr.setWarehouseId(warehouseId);
        if (notes != null) gr.setNotes(notes);

        List<Map<String, Object>> lineBodies = (List<Map<String, Object>>) body.get("lines");
        if (lineBodies != null) {
            grlRepo.deleteByGoodsReceiptId(id);
            for (Map<String, Object> lb : lineBodies) {
                Long skuId = ((Number) lb.get("skuId")).longValue();
                BigDecimal qtyOrdered = new BigDecimal(lb.get("qtyOrdered").toString());
                Long binId = ((Number) lb.get("binId")).longValue();

                TelemetryReading line = new TelemetryReading();
                line.setGoodsReceiptId(id);
                line.setSkuId(skuId);
                line.setQtyOrdered(qtyOrdered);
                line.setQtyReceived(BigDecimal.ZERO);
                line.setBinId(binId);
                grlRepo.save(line);
            }
        }

        return grRepo.save(gr);
    }

    @Transactional
    public void delete(Long id) {
        TelemetryBatch gr = grRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("TelemetryBatch not found: " + id));
        if (!"DRAFT".equals(gr.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be deleted");
        }
        grlRepo.deleteByGoodsReceiptId(id);
        grRepo.delete(gr);
    }

    @Transactional(rollbackFor = Exception.class)
    public TelemetryBatch complete(Long id, String bearerToken) {
        TelemetryBatch gr = grRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("TelemetryBatch not found: " + id));
        if (!"DRAFT".equals(gr.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be completed");
        }

        List<TelemetryReading> lines = grlRepo.findByGoodsReceiptId(id);
        if (lines.isEmpty()) {
            throw new RuntimeException("Cannot complete receipt with no lines");
        }

        for (TelemetryReading line : lines) {
            if (line.getQtyReceived().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("All lines must have qty_received > 0");
            }
        }

        // Write inventory adjustments — one per line. If any fails, transaction rolls back.
        for (TelemetryReading line : lines) {
            inventoryClient.postAdjustment(
                bearerToken,
                line.getSkuId(),
                gr.getWarehouseId(),
                line.getBinId(),
                line.getQtyReceived(),
                "RECEIPT",
                "PO",
                gr.getPurchaseOrderId()
            );
        }

        gr.setStatus("COMPLETED");
        gr.setCompletedAt(Instant.now());
        return grRepo.save(gr);
    }

    @Transactional
    public TelemetryReading updateLineQty(Long lineId, BigDecimal qtyReceived) {
        TelemetryReading line = grlRepo.findById(lineId)
                .orElseThrow(() -> new RuntimeException("TelemetryReading not found: " + lineId));
        TelemetryBatch gr = grRepo.findById(line.getGoodsReceiptId())
                .orElseThrow(() -> new RuntimeException("Parent receipt not found"));
        if (!"DRAFT".equals(gr.getStatus())) {
            throw new RuntimeException("Only DRAFT receipts can be modified");
        }
        if (qtyReceived.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("qtyReceived cannot be negative");
        }
        line.setQtyReceived(qtyReceived);
        return grlRepo.save(line);
    }

    private String findNextCode() {
        Optional<TelemetryBatch> last = grRepo.findTopByOrderByIdDesc();
        int nextNum = 1;
        if (last.isPresent() && last.get().getCode() != null && last.get().getCode().startsWith("GR-")) {
            try {
                nextNum = Integer.parseInt(last.get().getCode().substring(3)) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }
        return String.format("GR-%04d", nextNum);
    }

    private Map<String, Object> toMap(TelemetryBatch gr) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", gr.getId());
        map.put("code", gr.getCode());
        map.put("purchaseOrderId", gr.getPurchaseOrderId());
        map.put("warehouseId", gr.getWarehouseId());
        map.put("status", gr.getStatus());
        map.put("notes", gr.getNotes());
        map.put("receivedBy", gr.getReceivedBy());
        map.put("createdAt", gr.getCreatedAt());
        map.put("completedAt", gr.getCompletedAt());
        return map;
    }

    private Map<String, Object> lineToMap(TelemetryReading line) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", line.getId());
        map.put("goodsReceiptId", line.getGoodsReceiptId());
        map.put("skuId", line.getSkuId());
        map.put("qtyOrdered", line.getQtyOrdered());
        map.put("qtyReceived", line.getQtyReceived());
        map.put("binId", line.getBinId());
        return map;
    }
}
