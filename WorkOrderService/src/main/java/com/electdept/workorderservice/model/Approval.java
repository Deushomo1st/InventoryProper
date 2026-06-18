package com.electdept.workorderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "approval", schema = "workorder")
public class Approval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false) private Long purchaseOrderId;
    @Column(name = "approver_id", nullable = false) private Long approverId;
    @Column(nullable = false) private String action;
    private String reason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPurchaseOrderId() { return purchaseOrderId; } public void setPurchaseOrderId(Long p) { this.purchaseOrderId = p; }
    public Long getApproverId() { return approverId; } public void setApproverId(Long a) { this.approverId = a; }
    public String getAction() { return action; } public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant c) { this.createdAt = c; }
}
