package com.electdept.cablesizingservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cable_sizing_request", schema = "cable")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CableSizingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long skuId;

    @Column(name = "substation_id", nullable = false)
    private Long warehouseId;

    @Column(name = "reorder_point", nullable = false)
    private BigDecimal reorderPoint;

    @Column(name = "reorder_qty", nullable = false)
    private BigDecimal reorderQty;

    @Column(name = "preferred_supplier_id")
    private Long preferredSupplierId;

    @Column(name = "default_unit_price", nullable = false)
    private BigDecimal defaultUnitPrice;

    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public BigDecimal getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }

    public BigDecimal getReorderQty() { return reorderQty; }
    public void setReorderQty(BigDecimal reorderQty) { this.reorderQty = reorderQty; }

    public Long getPreferredSupplierId() { return preferredSupplierId; }
    public void setPreferredSupplierId(Long preferredSupplierId) { this.preferredSupplierId = preferredSupplierId; }

    public BigDecimal getDefaultUnitPrice() { return defaultUnitPrice; }
    public void setDefaultUnitPrice(BigDecimal defaultUnitPrice) { this.defaultUnitPrice = defaultUnitPrice; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
