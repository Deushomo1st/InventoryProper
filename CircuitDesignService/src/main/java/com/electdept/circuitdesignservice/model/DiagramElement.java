package com.electdept.circuitdesignservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "diagram_element", schema = "circuit")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiagramElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "circuit_diagram_id", nullable = false)
    private Long movementId;

    @Column(name = "asset_id", nullable = false)
    private Long skuId;

    @Column(name = "source_bin_id", nullable = false)
    private Long sourceBinId;

    @Column(name = "destination_bin_id", nullable = false)
    private Long destinationBinId;

    @Column(nullable = false)
    private BigDecimal qty;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Long getSourceBinId() { return sourceBinId; }
    public void setSourceBinId(Long sourceBinId) { this.sourceBinId = sourceBinId; }

    public Long getDestinationBinId() { return destinationBinId; }
    public void setDestinationBinId(Long destinationBinId) { this.destinationBinId = destinationBinId; }

    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
}
