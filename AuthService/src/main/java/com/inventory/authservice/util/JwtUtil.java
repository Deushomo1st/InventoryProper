package com.inventory.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final int expiryMinutes;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiry-minutes}") int expiryMinutes) {
        this.signingKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        this.expiryMinutes = expiryMinutes;
    }

    public String generateToken(Long userId, String email,
                                List<String> roles, List<String> permissions,
                                List<Integer> warehouseIds, boolean manager) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMinutes * 60_000L);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("perms", permissions)
                .claim("whs", warehouseIds)
                .claim("manager", manager)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public boolean getManager(Claims claims) {
        Boolean m = claims.get("manager", Boolean.class);
        return m != null && m;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Claims claims) {
        return claims.get("perms", List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getWarehouses(Claims claims) {
        return claims.get("whs", List.class);
    }
}