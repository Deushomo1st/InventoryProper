package com.electdept.notificationservice.repository;

import com.electdept.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Explicit JPQL — derived-name version was returning every row for the
    // user regardless of status (same shape as the attributes filter bug —
    // Spring Data's name parser silently produces a broken query for some
    // shapes). JPQL is unambiguous.
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.status = :status ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findAllByOrderByCreatedAtDesc();
}
