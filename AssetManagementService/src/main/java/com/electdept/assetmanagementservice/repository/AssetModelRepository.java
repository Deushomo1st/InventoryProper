package com.electdept.assetmanagementservice.repository;

import com.electdept.assetmanagementservice.model.AssetModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AssetModelRepository extends JpaRepository<AssetModel, Long> {
    Optional<AssetModel> findByCode(String code);
}