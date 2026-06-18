package com.electdept.scadaintegrationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "telemetry_batch", schema = "scada")
public class TelemetryBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String code;
    @Column(name = "work_order_id", nullable = false) private Long purchaseOrderId;
    @Column(name = "substation_id", nullable = false) private Long warehouseId;
    @Column(nullable = false) private String status = "DRAFT";
    private String notes;
    @Column(name = "received_by", nullable = false) private Long receivedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public Long getPurchaseOrderId() { return purchaseOrderId; } public void setPurchaseOrderId(Long p) { this.purchaseOrderId = p; }
    public Long getWarehouseId() { return warehouseId; } public void setWarehouseId(Long w) { this.warehouseId = w; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
    public Long getReceivedBy() { return receivedBy; } public void setReceivedBy(Long r) { this.receivedBy = r; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant c) { this.createdAt = c; }
    public Instant getCompletedAt() { return completedAt; } public void setCompletedAt(Instant c) { this.completedAt = c; }
}
