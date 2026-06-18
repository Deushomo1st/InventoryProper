package com.electdept.energymonitoringservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "meter_reading", schema = "energy")
public class MeterReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)       private Long skuId;
    @Column(name = "substation_id", nullable = false) private Long warehouseId;
    @Column(name = "pole_id", nullable = false)       private Long binId;
    @Column(nullable = false) private BigDecimal delta;
    @Column(name = "reason_code", nullable = false) private String reasonCode;
    @Column(name = "ref_doc_type") private String refDocType;
    @Column(name = "ref_doc_id")   private Long refDocId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; } public void setSkuId(Long s) { this.skuId = s; }
    public Long getWarehouseId() { return warehouseId; } public void setWarehouseId(Long w) { this.warehouseId = w; }
    public Long getBinId() { return binId; } public void setBinId(Long b) { this.binId = b; }
    public BigDecimal getDelta() { return delta; } public void setDelta(BigDecimal d) { this.delta = d; }
    public String getReasonCode() { return reasonCode; } public void setReasonCode(String r) { this.reasonCode = r; }
    public String getRefDocType() { return refDocType; } public void setRefDocType(String t) { this.refDocType = t; }
    public Long getRefDocId() { return refDocId; } public void setRefDocId(Long id) { this.refDocId = id; }
    public Long getUserId() { return userId; } public void setUserId(Long u) { this.userId = u; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
}
