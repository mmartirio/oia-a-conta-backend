package com.oiaaconta.order.client;

import com.oiaaconta.order.dto.auth.RestauranteLocalizacaoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Endpoint "/internal/**" do auth-service — sem JWT, confiável só por estar
// dentro da rede Docker interna (mesmo padrão que o AuthInternalClient do
// billing-service já usa pra bloquear/desbloquear restaurante).
@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/internal/restaurantes/{id}/localizacao")
    RestauranteLocalizacaoDto buscarLocalizacao(@PathVariable("id") Long id);
}
