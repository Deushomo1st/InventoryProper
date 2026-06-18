package com.electdept.assetmanagementservice.repository;

import com.electdept.assetmanagementservice.model.AssetSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssetSpecRepository extends JpaRepository<AssetSpec, Long> {
    // Explicit JPQL — derived-name version (findByProductId) was silently
    // returning all rows instead of filtering, likely because the entity has
    // `AssetModel product` (an association) and no scalar `productId` field, so
    // Spring Data couldn't derive the path. JPQL is unambiguous.
    @Query("SELECT a FROM AssetSpec a WHERE a.product.id = :productId")
    List<AssetSpec> findByProductId(@Param("productId") Long productId);
}