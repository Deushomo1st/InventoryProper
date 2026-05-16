package com.inventory.authservice.repository;

import com.inventory.authservice.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query(value = """
        SELECT p.code FROM auth.permission p
        JOIN auth.role_permission rp ON rp.permission_id = p.id
        WHERE rp.role_id IN (:roleIds)
    """, nativeQuery = true)
    List<String> findPermissionCodesByRoleIds(@Param("roleIds") List<Long> roleIds);

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    @Query(value = """
        SELECT p.* FROM auth.permission p
        JOIN auth.role_permission rp ON rp.permission_id = p.id
        WHERE rp.role_id = :roleId
    """, nativeQuery = true)
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}