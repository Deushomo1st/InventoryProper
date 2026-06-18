package com.electdept.scadaintegrationservice.repository;

import com.electdept.scadaintegrationservice.model.TelemetryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryBatchRepository extends JpaRepository<TelemetryBatch, Long> {
    List<TelemetryBatch> findByStatus(String status);
    List<TelemetryBatch> findByPurchaseOrderId(Long purchaseOrderId);
    Optional<TelemetryBatch> findTopByOrderByIdDesc();
}
