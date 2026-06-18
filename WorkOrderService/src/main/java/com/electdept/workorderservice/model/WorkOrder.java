package com.electdept.workorderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "work_order", schema = "workorder")
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String code;
    @Column(name = "document_id", nullable = false) private Long supplierId;
    @Column(name = "substation_id", nullable = false) private Long warehouseId;
    @Column(nullable = false) private String status = "DRAFT";
    private String notes;
    @Column(name = "total_amount") private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(nullable = false) private String currency = "USD";
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "closed_at") private Instant closedAt;

    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public Long getSupplierId() { return supplierId; } public void setSupplierId(Long s) { this.supplierId = s; }
    public Long getWarehouseId() { return warehouseId; } public void setWarehouseId(Long w) { this.warehouseId = w; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getTotalAmount() { return totalAmount; } public void setTotalAmount(BigDecimal t) { this.totalAmount = t; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long c) { this.createdBy = c; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant c) { this.createdAt = c; }
    public Instant getSubmittedAt() { return submittedAt; } public void setSubmittedAt(Instant s) { this.submittedAt = s; }
    public Instant getApprovedAt() { return approvedAt; } public void setApprovedAt(Instant a) { this.approvedAt = a; }
    public Instant getClosedAt() { return closedAt; } public void setClosedAt(Instant c) { this.closedAt = c; }
}
