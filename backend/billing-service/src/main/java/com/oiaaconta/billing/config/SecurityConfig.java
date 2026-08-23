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
                // O JWT é a ÚNICA fonte de identidade aqui — sem fallback pros headers
                // X-User-*. O gateway repassa o Authorization original intacto (só
                // ADICIONA os headers, não substitui a requisição), então toda chamada
                // legítima de usuário sempre carrega o token; um fallback pros headers
                // sem token permitiria que qualquer requisição dentro da rede interna
                // se autenticasse como qualquer usuário/role só forjando esses headers
                // e omitindo o Authorization. Chamada de serviço-a-serviço de verdade
                // (sem JWT de usuário) usa os endpoints /internal/**, que já são
                // permitAll — não precisam passar por aqui.
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
                chain.doFilter(req, res);
            }
        };
    }
}
