package com.oiaaconta.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrarLogRequest {
    @NotNull
    private Long restauranteId;

    @NotBlank
    private String tipo;

    @NotBlank
    private String descricao;

    private Long usuarioId;
    private String usuarioNome;
}
