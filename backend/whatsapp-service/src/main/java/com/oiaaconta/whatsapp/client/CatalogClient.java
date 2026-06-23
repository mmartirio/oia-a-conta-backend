package com.oiaaconta.whatsapp.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/categorias")
    List<CategoriaDto> listarCategorias(@RequestHeader("X-Restaurante-Id") Long restauranteId);

    @GetMapping("/api/produtos")
    List<ProdutoDto> listarProdutos(
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @RequestParam(required = false) Long categoriaId
    );

    @Data
    class CategoriaDto {
        private Long id;
        private String nome;
        private boolean ativo;
    }

    @Data
    class ProdutoDto {
        private Long id;
        private String nome;
        private String descricao;
        private BigDecimal preco;
        private boolean ativo;
        private Long categoriaId;
    }
}
