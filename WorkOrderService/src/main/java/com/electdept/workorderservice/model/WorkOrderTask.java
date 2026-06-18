package com.electdept.workorderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "work_order_task", schema = "workorder")
public class WorkOrderTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_id", nullable = false) private Long purchaseOrderId;
    @Column(name = "asset_id", nullable = false) private Long skuId;
    @Column(nullable = false) private BigDecimal qty;
    @Column(name = "unit_price", nullable = false) private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false) private BigDecimal lineTotal;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getPurchaseOrderId() { return purchaseOrderId; } public void setPurchaseOrderId(Long p) { this.purchaseOrderId = p; }
    public Long getSkuId() { return skuId; } public void setSkuId(Long s) { this.skuId = s; }
    public BigDecimal getQty() { return qty; } public void setQty(BigDecimal q) { this.qty = q; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal u) { this.unitPrice = u; }
    public BigDecimal getLineTotal() { return lineTotal; } public void setLineTotal(BigDecimal l) { this.lineTotal = l; }
}
