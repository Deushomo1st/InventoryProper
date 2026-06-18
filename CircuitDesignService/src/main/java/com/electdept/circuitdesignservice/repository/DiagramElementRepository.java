package com.electdept.circuitdesignservice.repository;

import com.electdept.circuitdesignservice.model.DiagramElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagramElementRepository extends JpaRepository<DiagramElement, Long> {
    List<DiagramElement> findByMovementId(Long movementId);
    void deleteByMovementId(Long movementId);
}
