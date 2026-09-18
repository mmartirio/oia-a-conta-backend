package com.oiaaconta.table.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "billing-service")
public interface BillingClient {

    @GetMapping("/internal/contratos/{restauranteId}/limites-plano")
    PlanoLimitesResponse buscarLimitesPlano(@PathVariable Long restauranteId);

    @GetMapping("/internal/contratos/restaurante/{restauranteId}/restricoes")
    RestricoesOperacaoResponse buscarRestricoesOperacao(@PathVariable Long restauranteId);

    @Data
    class PlanoLimitesResponse {
        private String funcionalidades;
        private Integer limiteUsuarios;
        private Integer limiteMesas;
    }

    // table-service só usa restringeModalidade/modalidadeOperacao; os
    // demais campos da resposta do billing-service são ignorados.
    @Data
    class RestricoesOperacaoResponse {
        private boolean restringeModalidade;
        private String modalidadeOperacao;
    }
}
