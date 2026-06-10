package com.comandadigital.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesaRequest {
    @NotNull(message = "Número da mesa obrigatório")
    @Min(value = 1, message = "Número da mesa deve ser maior que zero")
    private Integer numero;

    private Integer capacidade;
}
