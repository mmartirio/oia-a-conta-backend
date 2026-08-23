package com.oiaaconta.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VendaBalcaoRequest {

    @NotEmpty(message = "A venda deve ter pelo menos um item")
    @Valid
    private List<ItemRequest> itens;

    @NotNull(message = "Método de pagamento obrigatório")
    private String metodoPagamento;

    @Min(1) @Max(12)
    private Integer parcelas;

    private String observacao;

    // Cliente identificado na venda (opcional) — habilita cupom individual/de
    // grupo e promoções automáticas, e alimenta o gasto histórico do cliente.
    private Long clienteId;

    // Código de cupom informado pelo caixa (opcional) — validado no
    // catalog-service; se inválido, a venda é rejeitada (não é silenciosamente
    // ignorado, já que foi uma ação explícita do caixa).
    private String cupomCodigo;

    @Data
    public static class ItemRequest {
        // Obrigatório sse comboId não for informado.
        private Long produtoId;

        private String produtoNome;

        @NotNull(message = "Quantidade obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        private Integer quantidade;

        private BigDecimal precoUnitario;

        private String observacao;

        private Long comboId;
        private Integer comboQuantidade;
    }
}
