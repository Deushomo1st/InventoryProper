package com.inventory.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid failures on @RequestBody DTOs (e.g. missing email, invalid format).
     * Spring's default response is a generic blob — this turns it into a clean
     * { message, fields } payload the UI can render directly.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fields.put(fe.getField(), fe.getDefaultMessage())
        );

        // Pick the first field's message as the headline so the UI's
        // single-line error box reads naturally.
        String headline = fields.values().stream()
                .findFirst()
                .orElse("Validation failed");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", headline);
        body.put("fields", fields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Malformed / empty JSON bodies — caught before @Valid even runs.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Request body is missing or malformed"));
    }

    /**
     * Spring Security's @PreAuthorize throws AccessDeniedException on a failed
     * authority check. Spring's default for this is an opaque 403 — wrap it in
     * a themed-frontend-friendly message instead.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You don't have permission for this action. Manager rights required."));
    }
}
