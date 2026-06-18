package com.electdept.uomservice.repository;

import com.electdept.uomservice.model.Conversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {
    Optional<Conversion> findByFromUnitIdAndToUnitId(Long fromId, Long toId);
    void deleteAllByFromUnitId(Long unitId);
    void deleteAllByToUnitId(Long unitId);
}
