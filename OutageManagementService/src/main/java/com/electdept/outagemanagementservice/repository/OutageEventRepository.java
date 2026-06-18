package com.electdept.outagemanagementservice.repository;

import com.electdept.outagemanagementservice.model.OutageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutageEventRepository extends JpaRepository<OutageEvent, Long> {
    List<OutageEvent> findBySalesOrderId(Long salesOrderId);
    List<OutageEvent> findByStatus(String status);
    Optional<OutageEvent> findTopByOrderByIdDesc();
}
