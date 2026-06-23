package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class ResumoFinanceiroResponse {
    private BigDecimal totalComandas;
    private BigDecimal totalEntregas;
    private BigDecimal totalGeral;
    private Long qtdComandas;
    private Long qtdEntregas;
    private Map<String, BigDecimal> breakdownPorMetodo;
}
