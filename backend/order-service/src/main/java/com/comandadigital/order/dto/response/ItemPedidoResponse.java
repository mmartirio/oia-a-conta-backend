package com.comandadigital.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ItemPedidoResponse {
    private Long id;
    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;
    private String observacao;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;
}
