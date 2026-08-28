package com.oiaaconta.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/login",
        "/api/auth/registro",
        "/api/auth/google",
        "/api/auth/verificar-email",
        "/api/auth/reenviar-codigo",
        "/api/auth/publico",
        "/api/planos",
        "/api/links-sociais",
        "/api/catalog/publico",
        "/api/whatsapp/publico",
        "/api/configuracoes/pausas/status",
        "/ws",
        "/webhook",
        "/cardapio"
    );

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String nome = claims.get("nome") != null ? String.valueOf(claims.get("nome")) : "";

            // Camada adicional ao role — se o usuário tem um grupo, as
            // permissões dele (a) definem quais roles ele ganha (repassado
            // via X-User-Role, usado pelo billing-service) e (b) gateiam
            // diretamente aqui as rotas isoláveis por prefixo (ver
            // RegraPermissao — Cozinha/Comanda/Garçom/Delivery/Entregador/
            // Caixa não são isoláveis, ficam só com o grant de role).
            Object permissoesClaim = claims.get("permissoes");
            Set<String> permissoes = permissoesClaim instanceof Collection<?> col
                ? col.stream().map(String::valueOf).collect(Collectors.toSet())
                : Set.of();

            String roleHeader = !permissoes.isEmpty()
                ? String.join(",", PermissaoRoles.derivar(permissoes))
                : String.valueOf(claims.get("role"));

            if (!permissoes.isEmpty()) {
                String metodo = exchange.getRequest().getMethod().name();
                String permissaoRequerida = RegraPermissao.permissaoRequerida(path, metodo);
                if (permissaoRequerida != null && !permissoes.contains(permissaoRequerida)) {
                    log.warn("Acesso negado por permissão de grupo: {} {} exige {}", metodo, path, permissaoRequerida);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }

            ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header("X-User-Id", String.valueOf(claims.get("userId")))
                    .header("X-User-Nome", nome)
                    .header("X-Restaurante-Id", String.valueOf(claims.get("restauranteId")))
                    .header("X-User-Role", roleHeader))
                .build();

            return chain.filter(mutated);
        } catch (Exception e) {
            log.warn("JWT inválido: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
