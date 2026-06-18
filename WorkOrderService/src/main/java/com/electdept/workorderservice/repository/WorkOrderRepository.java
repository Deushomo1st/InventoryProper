package com.electdept.workorderservice.repository;

import com.electdept.workorderservice.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByStatus(String status);
    List<WorkOrder> findBySupplierId(Long supplierId);
    Optional<WorkOrder> findTopByOrderByIdDesc();
}
