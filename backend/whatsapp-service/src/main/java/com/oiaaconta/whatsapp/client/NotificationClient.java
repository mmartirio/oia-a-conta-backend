package com.oiaaconta.whatsapp.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/internal/notificar")
public interface NotificationClient {

    @PostMapping("/mensagem-whatsapp")
    ResponseEntity<Void> mensagemWhatsapp(@RequestBody NotificacaoMensagemWhatsapp body);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NotificacaoMensagemWhatsapp {
        private Long restauranteId;
        private String telefone;
        private String direcao;
        private String texto;
        private String criadoEm;
    }
}
