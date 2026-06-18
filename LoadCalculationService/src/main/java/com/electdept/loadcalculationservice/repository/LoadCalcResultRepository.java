package com.electdept.loadcalculationservice.repository;

import com.electdept.loadcalculationservice.model.LoadCalcResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoadCalcResultRepository extends JpaRepository<LoadCalcResult, Long> {
    List<LoadCalcResult> findByStatusOrderByCreatedAtDesc(String status);
    List<LoadCalcResult> findByRuleIdAndStatus(Long ruleId, String status);
}
