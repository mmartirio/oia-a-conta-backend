package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComboGrupoProdutoResponse {
    private Long produtoId;
    private String nome;
    private BigDecimal preco;
}
