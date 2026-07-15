package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AbrirCaixaRequest {
    @NotNull(message = "Valor de abertura obrigatório")
    @PositiveOrZero(message = "Valor de abertura não pode ser negativo")
    private BigDecimal valorAbertura;
}
