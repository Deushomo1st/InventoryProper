package com.inventory.authservice.repository;

import com.inventory.authservice.model.WarehouseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseAccessRepository extends JpaRepository<WarehouseAccess, Long> {

    @Query(value = """
        SELECT warehouse_id FROM auth.warehouse_access
        WHERE user_id = :userId
    """, nativeQuery = true)
    List<Long> findWarehouseIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndWarehouseId(Long userId, Long warehouseId);
}