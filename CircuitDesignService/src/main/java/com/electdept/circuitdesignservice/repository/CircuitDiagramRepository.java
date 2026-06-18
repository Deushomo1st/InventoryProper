package com.electdept.circuitdesignservice.repository;

import com.electdept.circuitdesignservice.model.CircuitDiagram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircuitDiagramRepository extends JpaRepository<CircuitDiagram, Long> {
    List<CircuitDiagram> findByStatus(String status);
    List<CircuitDiagram> findByWarehouseId(Long warehouseId);
    Optional<CircuitDiagram> findTopByOrderByIdDesc();
}
