package com.electdept.notificationservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public List<String> getRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<String> result = new ArrayList<>();
        Object roles = claims.get("roles");
        if (roles instanceof List) {
            ((List<?>) roles).forEach(r -> result.add(r.toString()));
        }
        return result;
    }

    public List<String> getPermissions(String token) {
        Claims claims = extractAllClaims(token);
        List<String> result = new ArrayList<>();
        Object perms = claims.get("perms");
        if (perms instanceof List) {
            ((List<?>) perms).forEach(p -> result.add(p.toString()));
        }
        return result;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
