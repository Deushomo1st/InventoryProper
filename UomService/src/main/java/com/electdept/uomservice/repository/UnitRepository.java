package com.electdept.uomservice.repository;

import com.electdept.uomservice.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    boolean existsByCode(String code);
    Optional<Unit> findByCode(String code);
}
