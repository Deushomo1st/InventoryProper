package com.electdept.billingtariffservice.repository;

import com.electdept.billingtariffservice.model.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TariffRateRepository extends JpaRepository<TariffRate, Long> {
    List<TariffRate> findByPriceListId(Long priceListId);
    Optional<TariffRate> findByPriceListIdAndSkuId(Long priceListId, Long skuId);
    void deleteByPriceListId(Long priceListId);
}
