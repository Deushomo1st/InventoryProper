package com.electdept.protectioncoordinationservice.repository;

import com.electdept.protectioncoordinationservice.model.DeviceSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceSelectionRepository extends JpaRepository<DeviceSelection, Long> {
    List<DeviceSelection> findBySalesOrderId(Long salesOrderId);
    void deleteBySalesOrderId(Long salesOrderId);
}
