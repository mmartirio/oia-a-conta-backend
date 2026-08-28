package com.oiaaconta.auth.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Só pode existir UM @FeignClient(name = "billing-service") no contexto do
// auth-service (Spring Cloud OpenFeign registra uma FeignClientSpecification
// por nome — uma segunda interface com o mesmo name quebra o boot com
// BeanDefinitionOverrideException) — por isso o método de limites do plano
// entra aqui em vez de um client novo, sem "path" fixo na anotação (cada
// método declara o caminho completo).
@FeignClient(name = "billing-service")
public interface AuditoriaClient {

    @PostMapping("/internal/auditoria")
    ResponseEntity<Void> registrar(@RequestBody RegistrarLogRequest request);

    @GetMapping("/internal/contratos/{restauranteId}/limites-plano")
    PlanoLimitesResponse buscarLimitesPlano(@PathVariable Long restauranteId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RegistrarLogRequest {
        private Long restauranteId;
        private String tipo;
        private String descricao;
        private Long usuarioId;
        private String usuarioNome;
    }

    @Data
    class PlanoLimitesResponse {
        private String funcionalidades;
        private Integer limiteUsuarios;
        private Integer limiteMesas;
    }
}
