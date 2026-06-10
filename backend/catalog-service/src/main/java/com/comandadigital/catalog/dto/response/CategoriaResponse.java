package com.comandadigital.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponse {
    private Long id;
    private Long restauranteId;
    private String nome;
    private boolean ativo;
}
