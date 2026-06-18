package com.electdept.documentmanagementservice.repository;

import com.electdept.documentmanagementservice.model.DocumentRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRevisionRepository extends JpaRepository<DocumentRevision, Long> {
    void deleteAllBySupplierId(Long supplierId);
}
