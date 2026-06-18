package com.electdept.scadaintegrationservice.repository;

import com.electdept.scadaintegrationservice.model.TelemetryReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TelemetryReadingRepository extends JpaRepository<TelemetryReading, Long> {
    List<TelemetryReading> findByGoodsReceiptId(Long goodsReceiptId);
    void deleteByGoodsReceiptId(Long goodsReceiptId);
}
