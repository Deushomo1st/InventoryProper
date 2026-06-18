package com.electdept.cablesizingservice.repository;

import com.electdept.cablesizingservice.model.CableSizingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CableSizingResultRepository extends JpaRepository<CableSizingResult, Long> {
    List<CableSizingResult> findByStatusOrderByCreatedAtDesc(String status);
    List<CableSizingResult> findByRuleIdAndStatus(Long ruleId, String status);
}
