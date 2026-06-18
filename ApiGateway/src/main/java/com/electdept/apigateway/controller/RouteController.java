package com.electdept.apigateway.controller;

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
            @Value("${services.asset.url:http://localhost:9002}") String assetUrl,
            @Value("${services.uom.url:http://localhost:9013}") String uomUrl,
            @Value("${services.gis.url:http://localhost:9004}") String gisUrl,
            @Value("${services.document.url:http://localhost:9005}") String documentUrl,
            @Value("${services.energy.url:http://localhost:9003}") String energyUrl,
            @Value("${services.workorder.url:http://localhost:9006}") String workorderUrl,
            @Value("${services.protection.url:http://localhost:9007}") String protectionUrl,
            @Value("${services.circuit.url:http://localhost:9008}") String circuitUrl,
            @Value("${services.scada.url:http://localhost:9009}") String scadaUrl,
            @Value("${services.outage.url:http://localhost:9010}") String outageUrl,
            @Value("${services.loadcalc.url:http://localhost:9011}") String loadcalcUrl,
            @Value("${services.audit.url:http://localhost:9012}") String auditUrl,
            @Value("${services.notification.url:http://localhost:9014}") String notificationUrl,
            @Value("${services.regulatory.url:http://localhost:9015}") String regulatoryUrl,
            @Value("${services.billing.url:http://localhost:9016}") String billingUrl,
            @Value("${services.cablesizing.url:http://localhost:9017}") String cablesizingUrl) {

        // Short timeouts so a hung downstream service doesn't freeze the menu's
        // /health pings (which run every 2s) or any other proxied call.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);  // 1s
        factory.setReadTimeout(2000);     // 2s
        this.restTemplate = new RestTemplate(factory);
        this.authServiceUrl = authServiceUrl;

        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("auth-service", authServiceUrl);
        urlsMap.put("asset", assetUrl);
        urlsMap.put("uom", uomUrl);
        urlsMap.put("gis", gisUrl);
        urlsMap.put("document", documentUrl);
        urlsMap.put("energy", energyUrl);
        urlsMap.put("workorder", workorderUrl);
        urlsMap.put("protection", protectionUrl);
        urlsMap.put("circuit", circuitUrl);
        urlsMap.put("scada", scadaUrl);
        urlsMap.put("outage", outageUrl);
        urlsMap.put("loadcalc", loadcalcUrl);
        urlsMap.put("audit", auditUrl);
        urlsMap.put("notification", notificationUrl);
        urlsMap.put("regulatory", regulatoryUrl);
        urlsMap.put("billing", billingUrl);
        urlsMap.put("cablesizing", cablesizingUrl);
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
                    .headers(safeHeaders(response.getHeaders()))
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
        String qs = request.getQueryString();
        String targetUrl = authServiceUrl + path + (qs != null ? "?" + qs : "");

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
                    .headers(safeHeaders(response.getHeaders()))
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

    // Forward any domain API request (/asset/**, /document/**, /uom/**, /gis/**, /energy/**,
    // /workorder/**, /protection/**, /circuit/**, /scada/**, /outage/**, /loadcalc/**, /audit/**,
    // /notification/**, /regulatory/**, /billing/**, /cablesizing/**) to the matching downstream
    // service. The path's first segment must be a key in serviceUrls.
    // /auth/** is handled separately above. /ui/** is handled below.
    @RequestMapping(
        value = "/{serviceName:asset|document|uom|gis|energy|workorder|protection|circuit|scada|outage|loadcalc|audit|notification|regulatory|billing|cablesizing}/**",
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
    )
    public ResponseEntity<?> forwardDomain(
            @PathVariable String serviceName,
            HttpServletRequest request,
            @RequestBody(required = false) String body) throws URISyntaxException {

        String targetBaseUrl = serviceUrls.get(serviceName);
        if (targetBaseUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Service not found: " + serviceName));
        }

        // Preserve query string when forwarding. request.getRequestURI() returns
        // only the path; without this every "?productId=", "?eventType=", "?status="
        // filter silently fails because the downstream sees no query params.
        String qs = request.getQueryString();
        String targetUrl = targetBaseUrl + request.getRequestURI() + (qs != null ? "?" + qs : "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    new URI(targetUrl), method, entity, String.class);
            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(safeHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
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
        // Preserve query string when forwarding. request.getRequestURI() returns
        // only the path; without this every "?productId=", "?eventType=", "?status="
        // filter silently fails because the downstream sees no query params.
        String qs = request.getQueryString();
        String targetUrl = targetBaseUrl + request.getRequestURI() + (qs != null ? "?" + qs : "");

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
                    .headers(safeHeaders(response.getHeaders()))
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

        // Forward the bearer token if the caller is authenticated. /auth/login is
        // public (no token sent), but /auth/register is now MANAGER-gated; without
        // this header pass-through, the downstream filter sees no principal and 403s.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    new URI(targetUrl),
                    entity,
                    String.class
            );

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(safeHeaders(response.getHeaders()))
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Use byte[] + explicit content-length so the body isn't dropped to
            // Content-Length: 0 by the String + application/json converter chain.
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Auth service is not reachable. Start it and try again."));
        }
    }

    /**
     * Strip hop-by-hop headers before re-emitting a proxied response.
     * Per RFC 7230 §6.1, Transfer-Encoding, Content-Length, and Connection are
     * hop-by-hop and must not be forwarded. Copying Transfer-Encoding from the
     * downstream causes a duplicate "Transfer-Encoding: chunked" on the gateway's
     * response (Spring re-applies it because the gateway also chunks), which
     * curl rejects as malformed chunked encoding (browsers tolerate it).
     */
    private HttpHeaders safeHeaders(HttpHeaders src) {
        HttpHeaders out = new HttpHeaders();
        src.forEach((name, values) -> {
            if (name == null) return;
            String lower = name.toLowerCase();
            if (lower.equals("transfer-encoding")
                    || lower.equals("content-length")
                    || lower.equals("connection")) {
                return;
            }
            out.put(name, values);
        });
        return out;
    }
}