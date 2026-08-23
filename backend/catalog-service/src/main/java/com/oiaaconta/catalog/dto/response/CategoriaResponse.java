package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponse {
    private Long id;
    private Long restauranteId;
    private String nome;
    private boolean ativo;
    private Integer ordem;
}
