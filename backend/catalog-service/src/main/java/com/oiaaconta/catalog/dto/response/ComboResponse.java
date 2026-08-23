package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ComboResponse {
    private Long id;
    private Long restauranteId;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private String imagemBase64;
    private boolean ativo;
    private List<ComboItemResponse> itens;
}
