package com.electdept.outagemanagementservice.repository;

import com.electdept.outagemanagementservice.model.AffectedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffectedAssetRepository extends JpaRepository<AffectedAsset, Long> {
    List<AffectedAsset> findByShipmentId(Long shipmentId);
    void deleteByShipmentId(Long shipmentId);
}
