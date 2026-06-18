package com.electdept.energymonitoringservice.repository;

import com.electdept.energymonitoringservice.model.CurrentReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CurrentReadingRepository extends JpaRepository<CurrentReading, Long> {
    Optional<CurrentReading> findBySkuIdAndWarehouseIdAndBinId(Long skuId, Long warehouseId, Long binId);
}
