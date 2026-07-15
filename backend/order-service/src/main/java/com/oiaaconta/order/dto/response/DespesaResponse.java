package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DespesaResponse {
    private Long id;
    private Long restauranteId;
    private String categoria;
    private String descricao;
    private BigDecimal valor;
    private LocalDate data;
    private LocalDateTime criadoEm;
}
