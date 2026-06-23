package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComissaoResponse {
    private Long funcionarioId;
    private String nome;
    private String role;
    private BigDecimal totalBase;
    private BigDecimal percentual;
    private BigDecimal valorComissao;
}
