package com.electdept.regulatoryreportingservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        return Long.parseLong(getClaim(token, Claims::getSubject));
    }

    public String getEmail(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public List<String> getRoles(String token) {
        return getClaims(token, claims -> claims.get("roles", List.class));
    }

    public List<String> getPermissions(String token) {
        return getClaims(token, claims -> claims.get("perms", List.class));
    }

    public List<Long> getWarehouses(String token) {
        return getClaims(token, claims -> claims.get("whs", List.class));
    }

    public Boolean isManager(String token) {
        return getClaim(token, claims -> claims.get("manager", Boolean.class));
    }

    private <T> T getClaim(String token, Function<Claims, T> claimsFunction) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claimsFunction.apply(claims);
    }

    private <T> T getClaims(String token, Function<Claims, T> claimsFunction) {
        return getClaim(token, claimsFunction);
    }
}
