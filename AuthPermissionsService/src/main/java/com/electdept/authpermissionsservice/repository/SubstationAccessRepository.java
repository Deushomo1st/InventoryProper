package com.electdept.authpermissionsservice.repository;

import com.electdept.authpermissionsservice.model.SubstationAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubstationAccessRepository extends JpaRepository<SubstationAccess, Long> {

    // The entity's @Column annotation says name = "substation_id" (see
    // SubstationAccess.java line 18), so Hibernate created the column
    // as substation_id, not warehouse_id. The native query MUST match
    // the actual DB column name. The Java method/parameter naming still
    // uses "warehouse" vocabulary so the existing callers (AuthService,
    // AuthController) keep compiling without changes — a proper rename
    // of the field, method, params, and controller endpoint paths is a
    // future pass.
    @Query(value = """
        SELECT substation_id FROM auth.substation_access
        WHERE user_id = :userId
    """, nativeQuery = true)
    List<Long> findWarehouseIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndWarehouseId(Long userId, Long warehouseId);
}
