package com.oiaaconta.whatsapp.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/internal/restaurantes/by-instance/{instanceName}")
    RestauranteInfo findByInstanceName(@PathVariable("instanceName") String instanceName);

    @GetMapping("/internal/restaurantes/{id}/whatsapp-instance")
    Map<String, String> getWhatsappInstance(@PathVariable("id") Long restauranteId);

    @PutMapping("/internal/restaurantes/{id}/whatsapp-instance")
    void salvarInstanciaWhatsapp(@PathVariable("id") Long restauranteId, @RequestBody Map<String, String> body);

    @Data
    class RestauranteInfo {
        private Long restauranteId;
        private boolean bloqueado;
        private boolean ativo;
    }
}
