package com.electdept.workorderservice.service;

import com.electdept.workorderservice.model.Approval;
import com.electdept.workorderservice.model.WorkOrder;
import com.electdept.workorderservice.model.WorkOrderTask;
import com.electdept.workorderservice.repository.ApprovalRepository;
import com.electdept.workorderservice.repository.WorkOrderTaskRepository;
import com.electdept.workorderservice.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private final WorkOrderRepository poRepo;
    private final WorkOrderTaskRepository lineRepo;
    private final ApprovalRepository approvalRepo;

    public WorkOrderService(WorkOrderRepository poRepo, WorkOrderTaskRepository lineRepo, ApprovalRepository approvalRepo) {
        this.poRepo = poRepo;
        this.lineRepo = lineRepo;
        this.approvalRepo = approvalRepo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkOrder po : poRepo.findAll()) {
            Map<String, Object> map = toMap(po);
            long lineCount = lineRepo.findByPurchaseOrderId(po.getId()).size();
            map.put("lineCount", lineCount);
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getById(Long id) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        Map<String, Object> map = toMap(po);
        List<WorkOrderTask> lines = lineRepo.findByPurchaseOrderId(id);
        map.put("lines", lines.stream().map(this::lineToMap).collect(Collectors.toList()));
        return map;
    }

    @Transactional
    public WorkOrder create(Map<String, Object> body, Long currentUserId) {
        String code = findNextCode();
        Long supplierId = ((Number) body.get("supplierId")).longValue();
        Long warehouseId = ((Number) body.get("warehouseId")).longValue();
        String notes = (String) body.get("notes");
        String currency = body.get("currency") != null ? (String) body.get("currency") : "USD";

        WorkOrder po = new WorkOrder();
        po.setCode(code);
        po.setSupplierId(supplierId);
        po.setWarehouseId(warehouseId);
        po.setStatus("DRAFT");
        po.setNotes(notes);
        po.setCurrency(currency);
        po.setCreatedBy(currentUserId);
        poRepo.save(po);

        List<Map<String, Object>> lineBodies = (List<Map<String, Object>>) body.get("lines");
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (lineBodies != null) {
            for (Map<String, Object> lb : lineBodies) {
                Long skuId = ((Number) lb.get("skuId")).longValue();
                BigDecimal qty = new BigDecimal(lb.get("qty").toString());
                BigDecimal unitPrice = new BigDecimal(lb.get("unitPrice").toString());
                BigDecimal lineTotal = qty.multiply(unitPrice);

                WorkOrderTask line = new WorkOrderTask();
                line.setPurchaseOrderId(po.getId());
                line.setSkuId(skuId);
                line.setQty(qty);
                line.setUnitPrice(unitPrice);
                line.setLineTotal(lineTotal);
                lineRepo.save(line);

                totalAmount = totalAmount.add(lineTotal);
            }
        }

        po.setTotalAmount(totalAmount);
        poRepo.save(po);
        return po;
    }

    @Transactional
    public WorkOrder update(Long id, Map<String, Object> body) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be updated");
        }

        Long supplierId = body.get("supplierId") != null ? ((Number) body.get("supplierId")).longValue() : null;
        Long warehouseId = body.get("warehouseId") != null ? ((Number) body.get("warehouseId")).longValue() : null;
        String notes = (String) body.get("notes");
        String currency = (String) body.get("currency");

        if (supplierId != null) po.setSupplierId(supplierId);
        if (warehouseId != null) po.setWarehouseId(warehouseId);
        if (notes != null) po.setNotes(notes);
        if (currency != null) po.setCurrency(currency);

        List<Map<String, Object>> lineBodies = (List<Map<String, Object>>) body.get("lines");
        if (lineBodies != null) {
            lineRepo.deleteByPurchaseOrderId(id);
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (Map<String, Object> lb : lineBodies) {
                Long skuId = ((Number) lb.get("skuId")).longValue();
                BigDecimal qty = new BigDecimal(lb.get("qty").toString());
                BigDecimal unitPrice = new BigDecimal(lb.get("unitPrice").toString());
                BigDecimal lineTotal = qty.multiply(unitPrice);

                WorkOrderTask line = new WorkOrderTask();
                line.setPurchaseOrderId(id);
                line.setSkuId(skuId);
                line.setQty(qty);
                line.setUnitPrice(unitPrice);
                line.setLineTotal(lineTotal);
                lineRepo.save(line);

                totalAmount = totalAmount.add(lineTotal);
            }
            po.setTotalAmount(totalAmount);
        }

        return poRepo.save(po);
    }

    @Transactional
    public void delete(Long id) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be deleted");
        }
        lineRepo.deleteByPurchaseOrderId(id);
        poRepo.delete(po);
    }

    @Transactional
    public WorkOrder submit(Long id) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException("Only DRAFT orders can be submitted");
        }
        po.setStatus("SUBMITTED");
        po.setSubmittedAt(Instant.now());
        return poRepo.save(po);
    }

    @Transactional
    public WorkOrder approve(Long id, Long approverId, String reason) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        if (!"SUBMITTED".equals(po.getStatus())) {
            throw new RuntimeException("Only SUBMITTED orders can be approved");
        }
        po.setStatus("APPROVED");
        po.setApprovedAt(Instant.now());
        poRepo.save(po);

        Approval approval = new Approval();
        approval.setPurchaseOrderId(id);
        approval.setApproverId(approverId);
        approval.setAction("APPROVED");
        approval.setReason(reason);
        approvalRepo.save(approval);

        return po;
    }

    @Transactional
    public WorkOrder reject(Long id, Long approverId, String reason) {
        WorkOrder po = poRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkOrder not found: " + id));
        if (!"SUBMITTED".equals(po.getStatus())) {
            throw new RuntimeException("Only SUBMITTED orders can be rejected");
        }
        po.setStatus("REJECTED");
        poRepo.save(po);

        Approval approval = new Approval();
        approval.setPurchaseOrderId(id);
        approval.setApproverId(approverId);
        approval.setAction("REJECTED");
        approval.setReason(reason);
        approvalRepo.save(approval);

        return po;
    }

    private String findNextCode() {
        Optional<WorkOrder> last = poRepo.findTopByOrderByIdDesc();
        int nextNum = 1;
        if (last.isPresent() && last.get().getCode() != null && last.get().getCode().startsWith("PO-")) {
            try {
                nextNum = Integer.parseInt(last.get().getCode().substring(3)) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }
        return String.format("PO-%04d", nextNum);
    }

    private Map<String, Object> toMap(WorkOrder po) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", po.getId());
        map.put("code", po.getCode());
        map.put("supplierId", po.getSupplierId());
        map.put("warehouseId", po.getWarehouseId());
        map.put("status", po.getStatus());
        map.put("notes", po.getNotes());
        map.put("totalAmount", po.getTotalAmount());
        map.put("currency", po.getCurrency());
        map.put("createdBy", po.getCreatedBy());
        map.put("createdAt", po.getCreatedAt());
        map.put("submittedAt", po.getSubmittedAt());
        map.put("approvedAt", po.getApprovedAt());
        map.put("closedAt", po.getClosedAt());
        return map;
    }

    private Map<String, Object> lineToMap(WorkOrderTask line) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", line.getId());
        map.put("purchaseOrderId", line.getPurchaseOrderId());
        map.put("skuId", line.getSkuId());
        map.put("qty", line.getQty());
        map.put("unitPrice", line.getUnitPrice());
        map.put("lineTotal", line.getLineTotal());
        return map;
    }
}
