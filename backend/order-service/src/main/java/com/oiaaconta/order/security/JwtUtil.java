package com.oiaaconta.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
            .parseSignedClaims(token).getPayload();
    }

    public Long extractUserId(String token) {
        Object val = extractClaims(token).get("userId");
        return val != null ? Long.valueOf(val.toString()) : null;
    }

    public String extractNome(String token) {
        return (String) extractClaims(token).get("nome");
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    public java.util.Set<String> extractPermissoes(String token) {
        Object val = extractClaims(token).get("permissoes");
        if (val instanceof java.util.Collection<?> col) {
            return col.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
        }
        return java.util.Set.of();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try { extractClaims(token); return true; } catch (Exception e) { return false; }
    }
}
