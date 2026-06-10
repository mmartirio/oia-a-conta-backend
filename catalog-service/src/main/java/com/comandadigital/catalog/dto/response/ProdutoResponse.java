package com.comandadigital.catalog.dto.response;

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
    private boolean ativo;
}
