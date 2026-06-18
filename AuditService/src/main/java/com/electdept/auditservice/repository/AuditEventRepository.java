package com.electdept.auditservice.repository;

import com.electdept.auditservice.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    // Explicit JPQL on the three filter methods — derived-name versions were
    // silently returning every row regardless of the filter (same shape as
    // the attributes/notifications filter bug). JPQL is unambiguous.
    @Query("SELECT a FROM AuditEvent a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<AuditEvent> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT a FROM AuditEvent a WHERE a.eventType = :eventType ORDER BY a.createdAt DESC")
    List<AuditEvent> findByEventTypeOrderByCreatedAtDesc(@Param("eventType") String eventType);

    @Query("SELECT a FROM AuditEvent a WHERE a.sourceService = :sourceService ORDER BY a.createdAt DESC")
    List<AuditEvent> findBySourceServiceOrderByCreatedAtDesc(@Param("sourceService") String sourceService);

    @Query("SELECT a FROM AuditEvent a WHERE a.createdAt >= :since AND a.createdAt < :until ORDER BY a.createdAt DESC")
    List<AuditEvent> findByCreatedAtBetween(@Param("since") java.time.Instant since, @Param("until") java.time.Instant until);

    boolean existsBySourceServiceAndSourceId(String sourceService, Long sourceId);
}
