package com.electdept.workorderservice.service;

import com.electdept.workorderservice.model.Approval;
import com.electdept.workorderservice.repository.ApprovalRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ApprovalService {

    private final ApprovalRepository repo;

    public ApprovalService(ApprovalRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Approval a : repo.findAll()) {
            result.add(toMap(a));
        }
        return result;
    }

    public List<Map<String, Object>> getByPurchaseOrderId(Long purchaseOrderId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Approval a : repo.findByPurchaseOrderId(purchaseOrderId)) {
            result.add(toMap(a));
        }
        return result;
    }

    private Map<String, Object> toMap(Approval a) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("purchaseOrderId", a.getPurchaseOrderId());
        map.put("approverId", a.getApproverId());
        map.put("action", a.getAction());
        map.put("reason", a.getReason());
        map.put("createdAt", a.getCreatedAt());
        return map;
    }
}
