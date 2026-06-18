package com.electdept.assetmanagementservice.repository;

import com.electdept.assetmanagementservice.model.AssetClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssetClassRepository extends JpaRepository<AssetClass, Long> {
    List<AssetClass> findByParentIsNull();
    List<AssetClass> findByParentId(Long parentId);
}