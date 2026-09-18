package com.oiaaconta.order.client;

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

// Sem "path" fixo na anotação — só pode existir UM @FeignClient(name =
// "billing-service") no contexto do order-service, então cada método
// declara o caminho completo (ver mesmo padrão em auth-service).
@FeignClient(name = "billing-service")
public interface AuditoriaClient {

    @PostMapping("/internal/auditoria")
    ResponseEntity<Void> registrar(@RequestBody RegistrarLogRequest request);

    @GetMapping("/internal/contratos/restaurante/{restauranteId}/restricoes")
    RestricoesOperacaoResponse buscarRestricoesOperacao(@PathVariable Long restauranteId);

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

    // order-service só usa restringeModalidade/modalidadeOperacao; os demais
    // campos da resposta do billing-service são ignorados.
    @Data
    class RestricoesOperacaoResponse {
        private boolean restringeModalidade;
        private String modalidadeOperacao;
    }
}
