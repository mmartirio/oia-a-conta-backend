package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConfiguracaoResponse {
    private Long restauranteId;
    private String pixChave;
    private BigDecimal comissaoGarcon;
    private BigDecimal comissaoEntregador;
    private BigDecimal comissaoCozinheiro;
    private boolean fechadoManualmente;
    private String motivoFechamentoManual;
    private String alertaPedidoSom;
    private boolean notificacaoWhatsappFalada;
    private BigDecimal freteTaxaBase;
    private BigDecimal freteValorPorKm;
}
