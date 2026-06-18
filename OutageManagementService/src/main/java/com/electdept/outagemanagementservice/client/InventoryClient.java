package com.electdept.outagemanagementservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class InventoryClient {

    @Value("${services.energy.url}")
    private String inventoryUrl;

    private final RestTemplate restTemplate;

    public InventoryClient() {
        this.restTemplate = new RestTemplate();
    }

    public void postAdjustment(String bearerToken, Long skuId, Long warehouseId, Long binId, 
                               BigDecimal delta, String reasonCode, String refDocType, Long refDocId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set("Authorization", bearerToken);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("skuId", skuId);
            body.put("warehouseId", warehouseId);
            body.put("binId", binId);
            body.put("delta", delta);
            body.put("reasonCode", reasonCode);
            body.put("refDocType", refDocType);
            body.put("refDocId", refDocId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(
                    inventoryUrl + "/energy/adjustments",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Inventory service error: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Inventory service is not reachable");
        }
    }
}
