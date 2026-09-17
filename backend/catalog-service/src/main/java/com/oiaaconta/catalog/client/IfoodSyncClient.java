package com.oiaaconta.catalog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// Espelha o IfoodClient do order-service — efeito colateral best-effort
// (chamada engolida em try/catch pelo call-site), nunca deve travar um
// CRUD de produto/combo por uma falha na integração externa. A grande
// maioria dos restaurantes não tem iFood vinculado; o endpoint chamado
// aqui é um no-op silencioso nesse caso (ver IfoodInternalController).
@FeignClient(name = "ifood-service")
public interface IfoodSyncClient {

    @PostMapping("/internal/ifood/catalogo/sincronizar/{restauranteId}")
    void sincronizarCatalogo(@PathVariable("restauranteId") Long restauranteId);
}
