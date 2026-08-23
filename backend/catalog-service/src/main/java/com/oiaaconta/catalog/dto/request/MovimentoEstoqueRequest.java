package com.oiaaconta.catalog.dto.request;

import com.oiaaconta.catalog.enums.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MovimentoEstoqueRequest {
    @NotNull(message = "Tipo obrigatório")
    private TipoMovimentacaoEstoque tipo; // ENTRADA, SAIDA ou AJUSTE (movimentação manual)

    @NotNull(message = "Quantidade obrigatória")
    private Integer quantidade; // sempre positivo; o sinal é definido pelo tipo

    private String motivo;
}
