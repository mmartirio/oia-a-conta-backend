package com.oiaaconta.order.dto.response;

import com.oiaaconta.order.enums.StatusSessaoCaixa;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SessaoCaixaResponse {
    private Long id;
    private String abertoPorNome;
    private BigDecimal valorAbertura;
    private LocalDateTime abertoEm;
    private String fechadoPorNome;
    private BigDecimal valorFechamento;
    private LocalDateTime fechadoEm;
    private StatusSessaoCaixa status;
}
