package com.oiaaconta.billing.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "auth-service")
public interface AuthInternalClient {

    @PutMapping("/internal/restaurantes/{id}/bloquear")
    void bloquear(@PathVariable("id") Long restauranteId);

    @PutMapping("/internal/restaurantes/{id}/desbloquear")
    void desbloquear(@PathVariable("id") Long restauranteId);
}
