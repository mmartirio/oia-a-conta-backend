package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FecharCaixaRequest {
    @NotNull(message = "Valor de fechamento obrigatório")
    @PositiveOrZero(message = "Valor de fechamento não pode ser negativo")
    private BigDecimal valorFechamento;
}
