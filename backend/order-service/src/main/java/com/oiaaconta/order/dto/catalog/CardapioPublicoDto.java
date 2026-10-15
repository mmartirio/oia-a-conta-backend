package com.oiaaconta.order.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Só o necessário do GET /api/catalog/publico/{id}/cardapio pra conferir o
// preço dos produtos de um pedido pago online (ver EntregaService.criar).
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardapioPublicoDto {
    private List<Categoria> categorias;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Categoria {
        private List<Produto> produtos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Produto {
        private Long id;
        private String nome;
        private BigDecimal preco;
        private boolean ativo;
    }
}
