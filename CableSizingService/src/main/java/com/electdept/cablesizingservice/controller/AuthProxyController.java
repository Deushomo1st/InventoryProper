package com.electdept.cablesizingservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
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
        return forward("/auth/login", HttpMethod.POST, body);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        return forward("/auth/register", HttpMethod.POST, body);
    }

    @RequestMapping(value = "/auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> authProxy(HttpServletRequest request) {
        String method = request.getMethod();
        HttpMethod httpMethod = HttpMethod.valueOf(method);

        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            return forward(request.getRequestURI(), httpMethod, null);
        } else {
            try {
                jakarta.servlet.ServletInputStream inputStream = request.getInputStream();
                byte[] bodyBytes = inputStream.readAllBytes();
                String bodyStr = new String(bodyBytes);
                
                if (bodyStr.isEmpty()) {
                    return forward(request.getRequestURI(), httpMethod, null);
                } else {
                    Map<String, Object> bodyMap = new HashMap<>();
                    // Will be populated if needed
                    return forward(request.getRequestURI(), httpMethod, bodyMap);
                }
            } catch (Exception e) {
                return forward(request.getRequestURI(), httpMethod, null);
            }
        }
    }

    private ResponseEntity<?> forward(String path, HttpMethod method, Object body) {
        String url = authUrl + path;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Object> entity = (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, method, entity, String.class);
            
            byte[] bodyBytes = response.getBody() != null ? response.getBody().getBytes(StandardCharsets.UTF_8) : new byte[0];
            
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            try {
                byte[] bodyBytes = e.getResponseBodyAsByteArray();
                return ResponseEntity.status(e.getStatusCode())
                        .contentType(MediaType.parseMediaType("application/json;charset=UTF-8"))
                        .contentLength(bodyBytes.length)
                        .body(bodyBytes);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\":\"Auth service is not reachable\"}");
        }
    }
}
