package com.oiaaconta.ifood.client;

import com.oiaaconta.ifood.dto.ifood.IfoodCatalogDto;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodCategoriaResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodItemRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Catalog API do iFood (v2.0, "Multisetup") — sem suporte a grupos de
// opção/complementos neste primeiro momento (nosso catálogo não modela isso,
// só Combo como pacote de preço fixo). A Multisetup exige um catalogId no
// caminho pra categorias (cada merchant pode ter mais de um catálogo, ex:
// DELIVERY vs INDOOR) — listarCatalogos() busca o(s) catálogo(s) da loja, e
// IfoodCatalogSyncService usa o primeiro (contexto DEFAULT/delivery). Itens
// já são por merchant direto (sem catalogId no caminho) — confirmado testando
// contra a API real, não é o que a doc deles deixa claro à primeira vista.
@FeignClient(name = "ifood-catalog", url = "${ifood.api-url}")
public interface IfoodCatalogClient {

    @GetMapping("/catalog/v2.0/merchants/{merchantId}/catalogs")
    List<IfoodCatalogDto> listarCatalogos(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId);

    @PostMapping("/catalog/v2.0/merchants/{merchantId}/catalogs/{catalogId}/categories")
    IfoodCategoriaResponse criarCategoria(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("catalogId") String catalogId,
        @RequestBody IfoodCategoriaRequest body);

    @PatchMapping("/catalog/v2.0/merchants/{merchantId}/catalogs/{catalogId}/categories/{categoryId}")
    void atualizarCategoria(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("catalogId") String catalogId,
        @PathVariable("categoryId") String categoryId,
        @RequestBody IfoodCategoriaRequest body);

    // PATCH aceita corpo parcial — confirmado na API real que só
    // {"status": "..."} já atualiza sem mexer em name/template/sequence.
    // Usado pra pausar categoria que sumiu do cardápio local sem precisar
    // reconstruir o corpo inteiro.
    @PatchMapping("/catalog/v2.0/merchants/{merchantId}/catalogs/{catalogId}/categories/{categoryId}")
    void atualizarStatusCategoria(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @PathVariable("catalogId") String catalogId,
        @PathVariable("categoryId") String categoryId,
        @RequestBody Map<String, String> body);

    // Cria OU atualiza (idempotente por item.id dentro do body) — não existe
    // POST/PUT separado por id no path pra item.
    @PutMapping("/catalog/v2.0/merchants/{merchantId}/items")
    void salvarItem(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @RequestBody IfoodItemRequest body);

    // itemId vai no corpo, não no caminho.
    @PatchMapping("/catalog/v2.0/merchants/{merchantId}/items/status")
    void atualizarStatusItem(
        @RequestHeader("Authorization") String bearerToken,
        @PathVariable("merchantId") String merchantId,
        @RequestBody Map<String, String> body);
}
