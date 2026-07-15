package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResumoDiaResponse {
    private Long total;
    private Long emProducao;
    private Long cancelados;
    private Long entregues;
    private BigDecimal totalCaixa;
    private BigDecimal ticketMedio;
}
