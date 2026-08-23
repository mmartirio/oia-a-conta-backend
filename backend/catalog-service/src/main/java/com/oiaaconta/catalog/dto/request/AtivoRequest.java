package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtivoRequest {
    @NotNull(message = "Campo 'ativo' obrigatório")
    private Boolean ativo;
}
