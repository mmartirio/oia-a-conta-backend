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
        // Obrigatório sse comboId não for informado — validado em
        // PedidoService (cada item precisa ser um produto OU um combo).
        private Long produtoId;

        @NotNull(message = "Quantidade obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        private Integer quantidade;

        private String observacao;

        private String produtoNome;

        private BigDecimal precoUnitario;

        // Preenchidos quando este item representa um Combo — nesse caso
        // produtoId/produtoNome/precoUnitario acima são ignorados: o
        // PedidoService expande o combo em N itens reais buscando a
        // composição no catalog-service. comboQuantidade = quantas unidades
        // do combo (default 1).
        private Long comboId;
        private Integer comboQuantidade;

        // Sabores escolhidos pelo cliente dentro de cada grupo do combo (ver
        // EscolhaSaborRequest) — vazio/nulo = usa o primeiro produto elegível
        // de cada grupo como padrão.
        private List<EscolhaSaborRequest> saboresEscolhidos;
    }
}
