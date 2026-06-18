package com.electdept.cablesizingservice.repository;

import com.electdept.cablesizingservice.model.CableSizingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CableSizingRequestRepository extends JpaRepository<CableSizingRequest, Long> {
    List<CableSizingRequest> findByActiveTrue();
}
