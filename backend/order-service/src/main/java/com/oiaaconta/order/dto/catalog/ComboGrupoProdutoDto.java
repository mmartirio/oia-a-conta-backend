package com.oiaaconta.order.dto.catalog;

import lombok.Data;

import java.math.BigDecimal;

// Espelha catalog-service.dto.response.ComboGrupoProdutoResponse.
@Data
public class ComboGrupoProdutoDto {
    private Long produtoId;
    private String nome;
    private BigDecimal preco;
}
