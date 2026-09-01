package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.response.CardapioPublicoResponse;
import com.oiaaconta.catalog.dto.response.ComboResponse;
import com.oiaaconta.catalog.dto.response.ProdutoNumeradoResponse;
import com.oiaaconta.catalog.service.CardapioService;
import com.oiaaconta.catalog.service.ComboService;
import com.oiaaconta.catalog.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/publico")
@RequiredArgsConstructor
public class PublicoCatalogController {

    private final CardapioService cardapioService;
    private final ComboService comboService;
    private final ProdutoService produtoService;

    @GetMapping("/{restauranteId}/cardapio")
    public ResponseEntity<CardapioPublicoResponse> getCardapio(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(cardapioService.getCardapio(restauranteId));
    }

    // Consumido pelo order-service (EntregaService) pra expandir combos nos
    // pedidos vindos do cardápio público/WhatsApp — esse fluxo não tem JWT de
    // usuário (é serviço-a-serviço, cliente anônimo), então não pode usar o
    // GET /api/combos/{id} administrativo (autenticado). O dado em si já é
    // público (mesmo combo que aparece no cardápio).
    @GetMapping("/{restauranteId}/combos/{id}")
    public ResponseEntity<ComboResponse> getCombo(@PathVariable Long restauranteId, @PathVariable Long id) {
        return ResponseEntity.ok(comboService.buscarPorId(restauranteId, id));
    }

    // Consumido pelo whatsapp-service pra montar/interpretar o cardápio
    // numerado enviado pelo chatbot (imagem que o admin desenha + números que
    // o cliente digita de volta no chat). Mesmo motivo de ser público: chamada
    // serviço-a-serviço sem JWT de usuário. Une produtos e combos no mesmo
    // espaço de numeração — sem isso, um número de combo (ex: 18) era
    // ignorado silenciosamente pelo chat, já que só produtos eram consultados.
    @GetMapping("/{restauranteId}/produtos-numerados")
    public ResponseEntity<List<ProdutoNumeradoResponse>> getProdutosNumerados(@PathVariable Long restauranteId) {
        List<ProdutoNumeradoResponse> unificado = new java.util.ArrayList<>(produtoService.listarNumerados(restauranteId));
        unificado.addAll(comboService.listarNumerados(restauranteId));
        unificado.sort(java.util.Comparator.comparing(ProdutoNumeradoResponse::getNumero));
        return ResponseEntity.ok(unificado);
    }
}
