package com.electdept.gismappingservice.repository;

import com.electdept.gismappingservice.model.Pole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PoleRepository extends JpaRepository<Pole, Long> {
    boolean existsByZoneIdAndCode(Long zoneId, String code);
    void deleteAllByZoneId(Long zoneId);
}
