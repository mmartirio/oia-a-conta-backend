package com.oiaaconta.auth.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "billing-service")
public interface BillingClient {

    @GetMapping("/internal/contratos/{restauranteId}/limites-plano")
    PlanoLimitesResponse buscarLimitesPlano(@PathVariable Long restauranteId);

    @Data
    class PlanoLimitesResponse {
        private String funcionalidades;
        private Integer limiteUsuarios;
        private Integer limiteMesas;
    }
}
