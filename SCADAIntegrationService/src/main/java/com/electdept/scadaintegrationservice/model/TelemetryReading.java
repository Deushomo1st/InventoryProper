package com.electdept.scadaintegrationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "telemetry_reading", schema = "scada")
public class TelemetryReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telemetry_batch_id", nullable = false) private Long goodsReceiptId;
    @Column(name = "asset_id", nullable = false) private Long skuId;
    @Column(name = "qty_ordered", nullable = false) private BigDecimal qtyOrdered;
    @Column(name = "qty_received", nullable = false) private BigDecimal qtyReceived = BigDecimal.ZERO;
    @Column(name = "pole_id", nullable = false) private Long binId;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getGoodsReceiptId() { return goodsReceiptId; } public void setGoodsReceiptId(Long g) { this.goodsReceiptId = g; }
    public Long getSkuId() { return skuId; } public void setSkuId(Long s) { this.skuId = s; }
    public BigDecimal getQtyOrdered() { return qtyOrdered; } public void setQtyOrdered(BigDecimal q) { this.qtyOrdered = q; }
    public BigDecimal getQtyReceived() { return qtyReceived; } public void setQtyReceived(BigDecimal q) { this.qtyReceived = q; }
    public Long getBinId() { return binId; } public void setBinId(Long b) { this.binId = b; }
}
