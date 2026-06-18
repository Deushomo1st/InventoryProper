package com.electdept.energymonitoringservice.repository;

import com.electdept.energymonitoringservice.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
}
