package com.oiaaconta.whatsapp.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "billing-service")
public interface BillingClient {

    @PostMapping("/internal/tickets/whatsapp")
    void registrarMensagemTicket(@RequestBody Map<String, String> body);

    @GetMapping("/internal/contratos/restaurante/{restauranteId}/restricoes")
    RestricoesOperacaoDto buscarRestricoes(@PathVariable("restauranteId") Long restauranteId);

    // whatsapp-service só usa limiteAtendentesWhatsapp; os demais campos da
    // resposta do billing-service são ignorados na desserialização.
    @Data
    class RestricoesOperacaoDto {
        private Integer limiteAtendentesWhatsapp;
    }
}
