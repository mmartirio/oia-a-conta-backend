package com.oiaaconta.catalog.dto.response;

import com.oiaaconta.catalog.enums.TipoDesconto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// Resposta de GET /api/cupons/validar — nunca é 4xx pra "não aplicável",
// só pra input malformado. valido=false + motivoInvalido explica a rejeição.
@Data
@Builder
public class CupomValidacaoResponse {
    private boolean valido;
    private String motivoInvalido;
    private Long cupomId;
    private String codigo;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
}
