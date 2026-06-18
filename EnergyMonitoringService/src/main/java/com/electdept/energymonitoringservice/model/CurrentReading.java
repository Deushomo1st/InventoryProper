package com.electdept.energymonitoringservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "current_reading", schema = "energy",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sku_id", "warehouse_id", "bin_id"}))
public class CurrentReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)       private Long skuId;
    @Column(name = "substation_id", nullable = false) private Long warehouseId;
    @Column(name = "pole_id", nullable = false)       private Long binId;
    @Column(name = "qty_on_hand", nullable = false)  private BigDecimal qtyOnHand = BigDecimal.ZERO;
    @Column(name = "last_updated", nullable = false) private LocalDateTime lastUpdated;

    @PrePersist @PreUpdate protected void touch() { lastUpdated = LocalDateTime.now(); }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; } public void setSkuId(Long s) { this.skuId = s; }
    public Long getWarehouseId() { return warehouseId; } public void setWarehouseId(Long w) { this.warehouseId = w; }
    public Long getBinId() { return binId; } public void setBinId(Long b) { this.binId = b; }
    public BigDecimal getQtyOnHand() { return qtyOnHand; } public void setQtyOnHand(BigDecimal q) { this.qtyOnHand = q; }
    public LocalDateTime getLastUpdated() { return lastUpdated; } public void setLastUpdated(LocalDateTime t) { this.lastUpdated = t; }
}
