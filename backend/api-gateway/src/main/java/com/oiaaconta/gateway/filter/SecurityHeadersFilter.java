package com.oiaaconta.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// O gateway é o único ponto público da aplicação e é WebFlux/reativo — não
// tem spring-boot-starter-security, então nenhum header padrão de segurança
// (os que o Spring Security aplicaria por padrão numa app MVC comum) é
// enviado. Este filtro adiciona o mínimo básico diretamente, sem precisar
// trazer Spring Security completo pra dentro da cadeia reativa do gateway.
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            var headers = response.getHeaders();
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
            headers.add("Cache-Control", "no-store");
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
