package com.oiaaconta.order.dto.request;

import com.oiaaconta.order.enums.CategoriaDespesa;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DespesaRequest {
    @NotNull(message = "Categoria obrigatória")
    private CategoriaDespesa categoria;

    private String descricao;

    @NotNull(message = "Valor obrigatório")
    private BigDecimal valor;

    @NotNull(message = "Data obrigatória")
    private LocalDate data;
}
