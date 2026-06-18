package com.electdept.workorderservice.repository;

import com.electdept.workorderservice.model.WorkOrderTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkOrderTaskRepository extends JpaRepository<WorkOrderTask, Long> {
    List<WorkOrderTask> findByPurchaseOrderId(Long purchaseOrderId);
    void deleteByPurchaseOrderId(Long purchaseOrderId);
}
