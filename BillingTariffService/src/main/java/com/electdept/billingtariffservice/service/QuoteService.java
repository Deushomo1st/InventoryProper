package com.electdept.billingtariffservice.service;

import com.electdept.billingtariffservice.model.RateAdjustment;
import com.electdept.billingtariffservice.model.TariffSchedule;
import com.electdept.billingtariffservice.model.TariffRate;
import com.electdept.billingtariffservice.repository.RateAdjustmentRepository;
import com.electdept.billingtariffservice.repository.TariffRateRepository;
import com.electdept.billingtariffservice.repository.TariffScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class QuoteService {

    private final TariffScheduleRepository priceListRepo;
    private final TariffRateRepository entryRepo;
    private final RateAdjustmentRepository discountRepo;

    public QuoteService(TariffScheduleRepository priceListRepo, TariffRateRepository entryRepo,
                        RateAdjustmentRepository discountRepo) {
        this.priceListRepo = priceListRepo;
        this.entryRepo = entryRepo;
        this.discountRepo = discountRepo;
    }

    public Map<String, Object> quote(String customerTier, Long skuId, BigDecimal qty) {
        // 1. Find active pricelist for this tier
        List<TariffSchedule> pricelists = priceListRepo.findByCustomerTierAndActiveTrue(customerTier);
        if (pricelists.isEmpty()) {
            throw new RuntimeException("No active pricelist for tier: " + customerTier);
        }
        TariffSchedule pl = pricelists.get(0);

        // 2. Find the entry for this SKU in that pricelist
        Optional<TariffRate> entryOpt = entryRepo.findByPriceListIdAndSkuId(pl.getId(), skuId);
        if (entryOpt.isEmpty()) {
            throw new RuntimeException("SKU " + skuId + " not in pricelist " + pl.getCode());
        }
        TariffRate entry = entryOpt.get();

        BigDecimal unitPrice = entry.getUnitPrice();
        BigDecimal lineSubtotal = unitPrice.multiply(qty);

        // 3. Find applicable active discounts for this SKU where min_qty <= qty
        List<RateAdjustment> candidates = discountRepo.findBySkuIdAndActiveTrue(skuId).stream()
                .filter(d -> d.getMinQty() == null || qty.compareTo(d.getMinQty()) >= 0)
                .toList();

        // 4. Compute best discount amount (largest savings wins)
        BigDecimal bestDiscountAmount = BigDecimal.ZERO;
        String bestDiscountCode = null;
        for (RateAdjustment d : candidates) {
            BigDecimal amount;
            if ("PERCENT".equals(d.getDiscountType())) {
                amount = lineSubtotal.multiply(d.getValue()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            } else {  // FIXED
                amount = d.getValue().multiply(qty);
            }
            if (amount.compareTo(bestDiscountAmount) > 0) {
                bestDiscountAmount = amount;
                bestDiscountCode = d.getCode();
            }
        }

        BigDecimal lineTotal = lineSubtotal.subtract(bestDiscountAmount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerTier", customerTier);
        result.put("priceListCode", pl.getCode());
        result.put("skuId", skuId);
        result.put("qty", qty);
        result.put("unitPrice", unitPrice);
        result.put("lineSubtotal", lineSubtotal);
        result.put("discountCode", bestDiscountCode);
        result.put("discountAmount", bestDiscountAmount);
        result.put("lineTotal", lineTotal);
        result.put("currency", pl.getCurrency());
        return result;
    }
}
