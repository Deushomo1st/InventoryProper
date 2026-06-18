package com.electdept.documentmanagementservice.repository;

import com.electdept.documentmanagementservice.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    boolean existsByCode(String code);
    Optional<Document> findByCode(String code);
}
