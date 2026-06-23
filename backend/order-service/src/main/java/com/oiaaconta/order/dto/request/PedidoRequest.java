package com.oiaaconta.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PedidoRequest {

    @NotEmpty(message = "O pedido deve ter pelo menos um item")
    @Valid
    private List<ItemRequest> itens;

    private String observacao;

    private boolean cozinha = true;

    @Data
    public static class ItemRequest {
        @NotNull(message = "Produto obrigatório")
        private Long produtoId;

        @NotNull(message = "Quantidade obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        private Integer quantidade;

        private String observacao;

        private String produtoNome;

        private BigDecimal precoUnitario;
    }
}
