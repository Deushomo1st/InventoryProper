package com.electdept.protectioncoordinationservice.repository;

import com.electdept.protectioncoordinationservice.model.CoordinationStudy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoordinationStudyRepository extends JpaRepository<CoordinationStudy, Long> {
    List<CoordinationStudy> findByStatus(String status);
    List<CoordinationStudy> findByCustomerId(Long customerId);
    Optional<CoordinationStudy> findTopByOrderByIdDesc();
}
