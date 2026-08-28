package com.oiaaconta.auth.security;

import com.oiaaconta.auth.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Usuario usuario) {
        var builder = Jwts.builder()
            .subject(usuario.getEmail())
            .claim("userId", usuario.getId())
            .claim("restauranteId", usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null)
            .claim("role", usuario.getRole().name())
            .claim("nome", usuario.getNome());

        // Camada adicional ao role — se o usuário tem um grupo, as permissões
        // dele viajam no token e passam a definir as authorities em cada
        // serviço (ver JwtAuthFilter), em vez do role sozinho.
        if (usuario.getGrupo() != null) {
            builder.claim("grupoId", usuario.getGrupo().getId());
            builder.claim("permissoes", usuario.getGrupo().getPermissoes());
        }

        return builder
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey())
            .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT inválido: {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
