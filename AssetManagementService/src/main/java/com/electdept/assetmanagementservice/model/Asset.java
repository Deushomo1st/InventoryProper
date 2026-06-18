package com.electdept.assetmanagementservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "asset", schema = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_model_id", nullable = false)
    private AssetModel product;

    @Column(name = "uom_id", nullable = false)
    private Long uomId; // Cross-schema FK, no JPA relationship

    // Free-text spec used to auto-generate the sku_code as
    // "<BRAND_SLUG>-<VARIANT>" (with -NNN suffix on collision).
    // Examples: "12OZ-CAN", "XPS13-16GB-512", "BLACK-MESH".
    @Column(length = 64)
    private String variant;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkuCode() { return skuCode; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetModel getProduct() { return product; }
    public void setProduct(AssetModel product) { this.product = product; }
    public Long getUomId() { return uomId; }
    public void setUomId(Long uomId) { this.uomId = uomId; }
    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}