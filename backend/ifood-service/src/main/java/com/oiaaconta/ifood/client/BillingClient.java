package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.billing.RestricoesOperacaoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Endpoint "/internal/**" do billing-service — sem JWT, confiável só por
// estar dentro da rede Docker interna (mesmo padrão do table/order-service).
@FeignClient(name = "billing-service")
public interface BillingClient {

    @GetMapping("/internal/contratos/restaurante/{restauranteId}/restricoes")
    RestricoesOperacaoDto buscarRestricoes(@PathVariable("restauranteId") Long restauranteId);
}
