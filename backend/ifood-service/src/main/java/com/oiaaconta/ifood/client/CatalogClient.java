package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.catalog.CardapioPublicoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Endpoint público do catalog-service (sem autenticação, mesmo usado pelo
// cardápio online) — resolvido via Eureka, não precisa de URL fixa.
@FeignClient(name = "catalog-service", path = "/api")
public interface CatalogClient {

    @GetMapping("/catalog/publico/{restauranteId}/cardapio")
    CardapioPublicoDto buscarCardapio(@PathVariable("restauranteId") Long restauranteId);
}
