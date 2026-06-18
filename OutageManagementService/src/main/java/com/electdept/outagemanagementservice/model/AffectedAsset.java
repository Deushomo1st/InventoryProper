package com.electdept.outagemanagementservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "affected_asset", schema = "outage")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AffectedAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "outage_event_id", nullable = false)
    private Long shipmentId;

    @Column(name = "asset_id", nullable = false)
    private Long skuId;

    @Column(name = "pole_id", nullable = false)
    private Long binId;

    @Column(name = "qty_ordered", nullable = false)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_shipped", nullable = false)
    private BigDecimal qtyShipped = BigDecimal.ZERO;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Long getBinId() { return binId; }
    public void setBinId(Long binId) { this.binId = binId; }

    public BigDecimal getQtyOrdered() { return qtyOrdered; }
    public void setQtyOrdered(BigDecimal qtyOrdered) { this.qtyOrdered = qtyOrdered; }

    public BigDecimal getQtyShipped() { return qtyShipped; }
    public void setQtyShipped(BigDecimal qtyShipped) { this.qtyShipped = qtyShipped; }
}
