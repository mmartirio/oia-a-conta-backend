package com.comandadigital.table.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesaResponse {
    private Long id;
    private Long restauranteId;
    private Integer numero;
    private Integer capacidade;
    private String status;
}
