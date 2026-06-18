package com.electdept.cablesizingservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cable_sizing_result", schema = "cable")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CableSizingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "asset_id", nullable = false)
    private Long skuId;

    @Column(name = "substation_id", nullable = false)
    private Long warehouseId;

    @Column(name = "current_qty", nullable = false)
    private BigDecimal currentQty;

    @Column(name = "suggested_qty", nullable = false)
    private BigDecimal suggestedQty;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_po_id")
    private Long createdPoId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public BigDecimal getCurrentQty() { return currentQty; }
    public void setCurrentQty(BigDecimal currentQty) { this.currentQty = currentQty; }

    public BigDecimal getSuggestedQty() { return suggestedQty; }
    public void setSuggestedQty(BigDecimal suggestedQty) { this.suggestedQty = suggestedQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCreatedPoId() { return createdPoId; }
    public void setCreatedPoId(Long createdPoId) { this.createdPoId = createdPoId; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getOrderedAt() { return orderedAt; }
    public void setOrderedAt(Instant orderedAt) { this.orderedAt = orderedAt; }

    public Instant getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(Instant dismissedAt) { this.dismissedAt = dismissedAt; }
}
