package com.oiaaconta.order.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxasMaquininhaRequest {
    private BigDecimal taxaDebito;
    private BigDecimal taxaCreditoVista;
    private BigDecimal taxaCreditoParcelado;
}
