package com.electdept.auditservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthProxyController {

    @Value("${services.auth.url}")
    private String authUrl;

    private final RestTemplate restTemplate;

    public AuthProxyController() {
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        return forward("/auth/login", HttpMethod.POST, body, false);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        return forward("/auth/register", HttpMethod.POST, body, false);
    }

    @RequestMapping(value = "/auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> proxyAuth(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String path = request.getRequestURI().replaceFirst("/auth", "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    authUrl + "/auth" + path,
                    method,
                    entity,
                    byte[].class
            );
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(response.getHeaders().getContentType());
            if (response.getHeaders().getContentLength() > 0) {
                responseHeaders.setContentLength(response.getHeaders().getContentLength());
            }
            
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity.status(e.getStatusCode())
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Auth service is not reachable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
    }

    private ResponseEntity<?> forward(String path, HttpMethod method, Map<String, Object> body, boolean includeAuth) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    authUrl + path,
                    method,
                    entity,
                    byte[].class
            );
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(response.getHeaders().getContentType());
            if (response.getHeaders().getContentLength() > 0) {
                responseHeaders.setContentLength(response.getHeaders().getContentLength());
            }
            
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity.status(e.getStatusCode())
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Auth service is not reachable");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
    }
}
