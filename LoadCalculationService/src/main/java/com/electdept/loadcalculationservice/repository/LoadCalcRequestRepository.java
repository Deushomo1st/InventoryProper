package com.electdept.loadcalculationservice.repository;

import com.electdept.loadcalculationservice.model.LoadCalcRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoadCalcRequestRepository extends JpaRepository<LoadCalcRequest, Long> {
    List<LoadCalcRequest> findByActiveTrue();
}
