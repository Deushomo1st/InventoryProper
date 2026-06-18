package com.electdept.scadaintegrationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class InventoryClient {

    private final String inventoryUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public InventoryClient(@Value("${services.energy.url}") String inventoryUrl) {
        this.inventoryUrl = inventoryUrl;
    }

    public void postAdjustment(String bearerToken, Long skuId, Long warehouseId, Long binId,
                               BigDecimal delta, String reasonCode,
                               String refDocType, Long refDocId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        }

        Map<String, Object> body = Map.of(
            "skuId", skuId,
            "warehouseId", warehouseId,
            "binId", binId,
            "delta", delta,
            "reasonCode", reasonCode,
            "refDocType", refDocType == null ? "" : refDocType,
            "refDocId", refDocId == null ? 0 : refDocId
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity(inventoryUrl + "/energy/adjustments", request, Map.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Inventory adjustment failed: " + e.getStatusCode()
                    + " " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("InventoryService is not reachable", e);
        }
    }
}
