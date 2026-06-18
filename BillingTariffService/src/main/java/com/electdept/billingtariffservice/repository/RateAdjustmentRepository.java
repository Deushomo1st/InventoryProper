package com.electdept.billingtariffservice.repository;

import com.electdept.billingtariffservice.model.RateAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateAdjustmentRepository extends JpaRepository<RateAdjustment, Long> {
    // Explicit JPQL — the derived-name `findBySkuIdAndActiveTrue` translates to
    // `WHERE sku_id = ?`, which silently excludes rows where sku_id IS NULL
    // (the "applies to any SKU" case). This union lets QuoteService see
    // both per-SKU discounts AND cross-SKU discounts in one shot.
    @Query("SELECT d FROM RateAdjustment d WHERE d.active = true AND (d.skuId = :skuId OR d.skuId IS NULL)")
    List<RateAdjustment> findBySkuIdAndActiveTrue(@Param("skuId") Long skuId);
}
