package com.electdept.cablesizingservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class InventoryClient {

    private final String inventoryUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    // Renamed during the ElectDept conversion: inventory → energy. The
    // property is defined in application.properties as services.energy.url
    // (pointing at EnergyMonitoringService on port 9003). The Java field
    // and class name still say "inventory" for now — a follow-up rename
    // pass should change them too.
    public InventoryClient(@Value("${services.energy.url}") String inventoryUrl) {
        this.inventoryUrl = inventoryUrl;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listLevels(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        if (bearerToken != null) headers.set(HttpHeaders.AUTHORIZATION, bearerToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<List> response = restTemplate.exchange(
                inventoryUrl + "/energy/levels", HttpMethod.GET, request, List.class);
            return (List<Map<String, Object>>) response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("Inventory listLevels failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("InventoryService is not reachable", e);
        }
    }
}
