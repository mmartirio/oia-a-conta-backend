package com.oiaaconta.billing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/planos").permitAll()
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/pagamentos/webhook").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public OncePerRequestFilter jwtFilter() {
        return new OncePerRequestFilter() {
            @Override
            @SuppressWarnings("null")
            protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest req,
                                            @org.springframework.lang.NonNull HttpServletResponse res,
                                            @org.springframework.lang.NonNull FilterChain chain)
                    throws ServletException, IOException {
                // Headers injetados pelo gateway — X-User-Role pode vir com
                // vários roles separados por vírgula quando o usuário tem um
                // Grupo (ver JwtAuthFilter do api-gateway, que já resolve as
                // permissões do grupo em roles antes de repassar pra cá).
                String role = req.getHeader("X-User-Role");
                String userId = req.getHeader("X-User-Id");
                String nome = req.getHeader("X-User-Nome");
                String restauranteId = req.getHeader("X-Restaurante-Id");

                if (role != null && userId != null) {
                    String principal = nome != null ? nome : userId;
                    List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(role.split(","))
                        .map(String::trim).filter(r -> !r.isEmpty())
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(restauranteId);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    String authHeader = req.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                            Claims claims = Jwts.parser().verifyWith(key).build()
                                .parseSignedClaims(authHeader.substring(7)).getPayload();
                            Object permissoesClaim = claims.get("permissoes");
                            List<SimpleGrantedAuthority> authorities;
                            if (permissoesClaim instanceof java.util.Collection<?> col && !col.isEmpty()) {
                                java.util.Set<String> permissoes = col.stream()
                                    .map(String::valueOf).collect(java.util.stream.Collectors.toSet());
                                authorities = com.oiaaconta.billing.security.PermissaoRoles.derivar(permissoes).stream()
                                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                            } else {
                                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
                            }
                            var authToken = new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        } catch (Exception e) {
                            log.warn("JWT inválido: {}", e.getMessage());
                        }
                    }
                }
                chain.doFilter(req, res);
            }
        };
    }
}
