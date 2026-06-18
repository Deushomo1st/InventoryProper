package com.electdept.protectioncoordinationservice.repository;

import com.electdept.protectioncoordinationservice.model.ProtectionScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProtectionSchemeRepository extends JpaRepository<ProtectionScheme, Long> {
}
