package com.oiaaconta.order.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConfiguracaoRequest {
    private String pixChave;
    private BigDecimal comissaoGarcon;
    private BigDecimal comissaoEntregador;
    private BigDecimal comissaoCozinheiro;
    private String alertaPedidoSom;
}
