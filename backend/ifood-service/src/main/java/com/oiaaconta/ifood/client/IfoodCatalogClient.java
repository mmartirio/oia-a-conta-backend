package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodItemRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Catalog API do iFood (v2.0) — sem suporte a grupos de opção/complementos
// neste primeiro momento (nosso catálogo não modela isso, só Combo como
// pacote de preço fixo). Nomes/paths conferidos pelo meu conhecimento geral
// da API, não uma consulta à doc atual.
@FeignClient(name = "ifood-catalog", url = "${ifood.api-url}")
public interface IfoodCatalogClient {

    @PostMapping("/catalog/v2.0/merchants/{merchantId}/categories")
    IfoodCategoriaResponse criarCategoria(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @RequestBody IfoodCategoriaRequest body);

    @PutMapping("/catalog/v2.0/merchants/{merchantId}/categories/{categoryId}")
    void atualizarCategoria(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("categoryId") String categoryId,
        @RequestBody IfoodCategoriaRequest body);

    @PostMapping("/catalog/v2.0/merchants/{merchantId}/items")
    IfoodItemResponse criarItem(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @RequestBody IfoodItemRequest body);

    @PutMapping("/catalog/v2.0/merchants/{merchantId}/items/{itemId}")
    void atualizarItem(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("itemId") String itemId,
        @RequestBody IfoodItemRequest body);

    @PatchMapping("/catalog/v2.0/merchants/{merchantId}/items/{itemId}/status")
    void atualizarStatusItem(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("itemId") String itemId,
        @RequestBody Map<String, String> body);
}
