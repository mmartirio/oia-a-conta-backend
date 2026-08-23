package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProdutoResponse {
    private Long id;
    private Long restauranteId;
    private Long categoriaId;
    private String categoriaNome;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String imagemBase64;
    private Integer numeroCardapio;
    private boolean ativo;
}
