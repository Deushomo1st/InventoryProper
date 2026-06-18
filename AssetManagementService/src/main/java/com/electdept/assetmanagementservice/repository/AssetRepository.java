package com.electdept.assetmanagementservice.repository;

import com.electdept.assetmanagementservice.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySkuCode(String skuCode);
    List<Asset> findByProductId(Long productId);
    // Used during auto-code generation to detect collisions within the same
    // <BRAND>-<VARIANT> prefix and append a -NNN suffix.
    List<Asset> findBySkuCodeStartingWith(String prefix);
}