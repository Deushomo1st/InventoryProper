package com.electdept.documentmanagementservice.repository;

import com.electdept.documentmanagementservice.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    void deleteAllBySupplierId(Long supplierId);
}
