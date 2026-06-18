package com.electdept.gismappingservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AuthProxyController {

    private final String authServiceUrl;
    private final RestTemplate restTemplate;

    public AuthProxyController(@Value("${services.auth.url:http://localhost:9001}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> forwardLogin(@RequestBody String body) {
        return forward(body, "/auth/login", "POST", null);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> forwardRegister(@RequestBody String body) {
        return forward(body, "/auth/register", "POST", null);
    }

    @RequestMapping(
        value = "/auth/**",
        method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
    )
    public ResponseEntity<?> forwardAuthApi(HttpServletRequest request,
                                            @RequestBody(required = false) String body) {
        return forward(body, request.getRequestURI(), request.getMethod(), request.getHeader("Authorization"));
    }

    private ResponseEntity<?> forward(String body, String path, String method, String authHeader) {
        String targetUrl = authServiceUrl + path;
        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                java.net.URI.create(targetUrl),
                HttpMethod.valueOf(method),
                entity,
                String.class
            );
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            byte[] bodyBytes = e.getResponseBodyAsByteArray();
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } catch (ResourceAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"message\": \"Auth service is not reachable. Start it and try again.\"}");
        }
    }
}
