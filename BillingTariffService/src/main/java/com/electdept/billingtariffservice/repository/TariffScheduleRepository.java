package com.electdept.billingtariffservice.repository;

import com.electdept.billingtariffservice.model.TariffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TariffScheduleRepository extends JpaRepository<TariffSchedule, Long> {
    List<TariffSchedule> findByCustomerTierAndActiveTrue(String customerTier);
}
