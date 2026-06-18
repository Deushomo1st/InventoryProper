package com.electdept.gismappingservice.repository;

import com.electdept.gismappingservice.model.Substation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SubstationRepository extends JpaRepository<Substation, Long> {
    boolean existsByCode(String code);
    Optional<Substation> findByCode(String code);
}
