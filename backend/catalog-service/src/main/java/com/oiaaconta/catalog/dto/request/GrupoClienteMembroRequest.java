package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrupoClienteMembroRequest {
    @NotNull(message = "Cliente obrigatório")
    private Long clienteId;
}
