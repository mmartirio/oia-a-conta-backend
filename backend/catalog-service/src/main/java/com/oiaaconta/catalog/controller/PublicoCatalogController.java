package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.response.CategoriaResponse;
import com.oiaaconta.catalog.dto.response.ProdutoResponse;
import com.oiaaconta.catalog.service.CategoriaService;
import com.oiaaconta.catalog.service.ProdutoService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/publico")
@RequiredArgsConstructor
public class PublicoCatalogController {

    private final CategoriaService categoriaService;
    private final ProdutoService produtoService;

    @GetMapping("/{restauranteId}/cardapio")
    public ResponseEntity<List<CategoriaComProdutos>> getCardapio(@PathVariable Long restauranteId) {
        List<CategoriaResponse> categorias = categoriaService.listar(restauranteId);

        List<CategoriaComProdutos> resultado = categorias.stream()
            .filter(CategoriaResponse::isAtivo)
            .map(c -> {
                List<ProdutoResponse> produtos = produtoService
                    .listarPorCategoria(restauranteId, c.getId())
                    .stream()
                    .filter(ProdutoResponse::isAtivo)
                    .toList();
                return CategoriaComProdutos.builder()
                    .id(c.getId())
                    .nome(c.getNome())
                    .produtos(produtos)
                    .build();
            })
            .filter(c -> !c.getProdutos().isEmpty())
            .toList();

        return ResponseEntity.ok(resultado);
    }

    @Data
    @Builder
    public static class CategoriaComProdutos {
        private Long id;
        private String nome;
        private List<ProdutoResponse> produtos;
    }
}
