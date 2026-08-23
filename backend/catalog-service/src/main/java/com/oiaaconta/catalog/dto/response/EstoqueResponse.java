package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EstoqueResponse {
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private int quantidadeMinima;
    private boolean controlado;
    private boolean abaixoDoMinimo;
}
