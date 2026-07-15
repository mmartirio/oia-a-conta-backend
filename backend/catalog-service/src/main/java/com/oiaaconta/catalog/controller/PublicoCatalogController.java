package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.response.CardapioCategoriaResponse;
import com.oiaaconta.catalog.service.CardapioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/publico")
@RequiredArgsConstructor
public class PublicoCatalogController {

    private final CardapioService cardapioService;

    @GetMapping("/{restauranteId}/cardapio")
    public ResponseEntity<List<CardapioCategoriaResponse>> getCardapio(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(cardapioService.getCardapio(restauranteId));
    }
}
