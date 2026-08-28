package com.oiaaconta.billing.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanoLimitesResponse {
    private String funcionalidades;
    private Integer limiteUsuarios;
    private Integer limiteMesas;
}
