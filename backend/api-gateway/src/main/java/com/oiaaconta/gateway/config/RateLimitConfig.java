package com.oiaaconta.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // ÚLTIMO IP do X-Forwarded-For = o que o nginx do host acrescenta
            // com o IP real da conexão. O primeiro vem do próprio cliente — usar
            // ele deixava qualquer um trocar o valor a cada requisição e nunca
            // bater no limite.
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] ips = forwarded.split(",");
                return Mono.just(ips[ips.length - 1].trim());
            }
            var remote = exchange.getRequest().getRemoteAddress();
            if (remote != null) {
                return Mono.just(remote.getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }
}
