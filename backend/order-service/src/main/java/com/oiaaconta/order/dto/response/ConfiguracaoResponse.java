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
}
