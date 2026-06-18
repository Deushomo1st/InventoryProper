package com.electdept.authpermissionsservice.repository;

import com.electdept.authpermissionsservice.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query(value = """
    SELECT r.id FROM auth.role r
    JOIN auth.user_role ur ON ur.role_id = r.id
    WHERE ur.user_id = :userId
""", nativeQuery = true)
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT r.name FROM auth.role r
        JOIN auth.user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = :userId
    """, nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}