package com.oiaaconta.auth.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "billing-service", path = "/internal/auditoria")
public interface AuditoriaClient {

    @PostMapping
    ResponseEntity<Void> registrar(@RequestBody RegistrarLogRequest request);

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
}
