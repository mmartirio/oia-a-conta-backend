package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EvolucaoDiariaResponse {
    private LocalDate data;
    private BigDecimal totalComandas;
    private BigDecimal totalEntregas;
    private BigDecimal totalDespesas;
}
