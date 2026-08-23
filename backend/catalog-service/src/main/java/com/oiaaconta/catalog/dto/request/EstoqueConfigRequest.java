package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstoqueConfigRequest {
    @NotNull(message = "Quantidade mínima obrigatória")
    @Min(value = 0, message = "Quantidade mínima não pode ser negativa")
    private Integer quantidadeMinima;

    @NotNull(message = "Campo 'controlado' obrigatório")
    private Boolean controlado;
}
