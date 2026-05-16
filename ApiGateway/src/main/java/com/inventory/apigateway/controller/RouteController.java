package com.inventory.apigateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RouteController {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final Map<String, String> serviceUrls;

    public RouteController(
            @Value("${services.auth.url:http://localhost:9001}") String authServiceUrl,
            @Value("${services.catalog.url:http://localhost:9002}") String catalogUrl,
            @Value("${services.uom.url:http://localhost:9013}") String uomUrl,
            @Value("${services.warehouse.url:http://localhost:9004}") String warehouseUrl,
            @Value("${services.supplier.url:http://localhost:9005}") String supplierUrl,
            @Value("${services.inventory.url:http://localhost:9003}") String inventoryUrl,
            @Value("${services.procurement.url:http://localhost:9006}") String procurementUrl,
            @Value("${services.sales.url:http://localhost:9007}") String salesUrl,
            @Value("${services.movement.url:http://localhost:9008}") String movementUrl,
            @Value("${services.receiving.url:http://localhost:9009}") String receivingUrl,
            @Value("${services.fulfillment.url:http://localhost:9010}") String fulfillmentUrl,
            @Value("${services.replenishment.url:http://localhost:9011}") String replenishmentUrl,
            @Value("${services.audit.url:http://localhost:9012}") String auditUrl,
            @Value("${services.notification.url:http://localhost:9014}") String notificationUrl,
            @Value("${services.reporting.url:http://localhost:9015}") String reportingUrl,
            @Value("${services.pricing.url:http://localhost:9016}") String pricingUrl) {
        
        // Short timeouts so a hung downstream service doesn't freeze the menu's
        // /health pings (which run every 2s) or any other proxied call.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);  // 1s
        factory.setReadTimeout(2000);     // 2s
        this.restTemplate = new RestTemplate(factory);
        this.authServiceUrl = authServiceUrl;
        
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("auth-service", authServiceUrl);
        urlsMap.put("catalog", catalogUrl);
        urlsMap.put("uom", uomUrl);
        urlsMap.put("warehouse", warehouseUrl);
        urlsMap.put("supplier", supplierUrl);
        urlsMap.put("inventory", inventoryUrl);
        urlsMap.put("procurement", procurementUrl);
        urlsMap.put("sales", salesUrl);
        urlsMap.put("movement", movementUrl);
        urlsMap.put("receiving", receivingUrl);
        urlsMap.put("fulfillment", fulfillmentUrl);
        urlsMap.put("replenishment", replenishmentUrl);
        urlsMap.put("audit", auditUrl);
        urlsMap.put("notification", notificationUrl);
        urlsMap.put("reporting", reportingUrl);
        urlsMap.put("pricing", pricingUrl);
        this.serviceUrls = urlsMap;
    }

    // Redirect root to login page
    @GetMapping("/")
    public void redirectToLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/login.html");
    }

    // Generic health check route: /health/{serviceName} -> {service}/health
    @GetMapping("/health/{serviceName}")
    public ResponseEntity<?> forwardHealth(@PathVariable String serviceName) {
        String targetBaseUrl = serviceUrls.get(serviceName);
        if (targetBaseUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Service not found: " + serviceName));
        }
        
        try {
            String targetUrl = targetBaseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(
                    new URI(targetUrl),
                    String.class
            );
            
            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }

    // Forward login requests to auth-service
    @RequestMapping(value = "/auth/login", method = RequestMethod.POST)
    public ResponseEntity<?> forwardLogin(
            HttpServletRequest request,
            @RequestBody String body) throws URISyntaxException {

        return forward(request, body, authServiceUrl + "/auth/login");
    }

    // Forward register requests to auth-service
    @RequestMapping(value = "/auth/register", method = RequestMethod.POST)
    public ResponseEntity<?> forwardRegister(
            HttpServletRequest request,
            @RequestBody String body) throws URISyntaxException {

        return forward(request, body, authServiceUrl + "/auth/register");
    }

    // Forward all other /auth/** API requests to auth-service
    @RequestMapping(value = "/auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> forwardAuthApi(
            HttpServletRequest request,
            @RequestBody(required = false) String body) throws URISyntaxException {
        
        String path = request.getRequestURI();
        String targetUrl = authServiceUrl + path;

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        
        // Forward authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    new URI(targetUrl),
                    method,
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Downstream returned 4xx/5xx — forward its status + body intact.
            // Use byte[] (not String) so ByteArrayHttpMessageConverter writes the
            // bytes directly; with a String body + application/json content-type,
            // the converter chain has historically dropped the body to Content-Length: 0.
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            // Connection refused / read timeout / DNS failure — downstream is unreachable.
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Service is not reachable. Start it and try again."));
        }
    }

    // Forward UI requests to downstream services
    @RequestMapping(value = "/ui/{serviceName}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> forwardToService(
            @PathVariable String serviceName,
            HttpServletRequest request,
            @RequestBody(required = false) String body) throws URISyntaxException {
        
        String targetBaseUrl = serviceUrls.get(serviceName);
        if (targetBaseUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Service not found: " + serviceName));
        }

        // Forward the full URI - don't strip the /ui/{serviceName} prefix
        String targetUrl = targetBaseUrl + request.getRequestURI();

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = new HttpHeaders();
        
        // Only set Content-Type when there's a body
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        
        // Forward authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    new URI(targetUrl),
                    method,
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Downstream returned 4xx/5xx — forward its status + body intact.
            // Use byte[] (not String) so ByteArrayHttpMessageConverter writes the
            // bytes directly; with a String body + application/json content-type,
            // the converter chain has historically dropped the body to Content-Length: 0.
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            // Connection refused / read timeout / DNS failure — downstream is unreachable.
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Service is not reachable. Start it and try again."));
        }
    }

    private ResponseEntity<?> forward(HttpServletRequest request,
                                      String body,
                                      String targetUrl) throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    new URI(targetUrl),
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Auth service is not reachable. Start it and try again."));
        }
    }
}