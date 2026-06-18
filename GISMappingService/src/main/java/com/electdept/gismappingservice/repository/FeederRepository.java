package com.electdept.gismappingservice.repository;

import com.electdept.gismappingservice.model.Feeder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FeederRepository extends JpaRepository<Feeder, Long> {
    boolean existsByWarehouseIdAndCode(Long warehouseId, String code);
    void deleteAllByWarehouseId(Long warehouseId);
}
