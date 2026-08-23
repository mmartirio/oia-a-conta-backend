package com.oiaaconta.order.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FreteConfigRequest {
    private BigDecimal freteTaxaBase;
    private BigDecimal freteValorPorKm;
}
