package com.oiaaconta.order.dto.catalog;

import com.oiaaconta.order.enums.TipoDesconto;
import lombok.Data;

import java.math.BigDecimal;

// Espelha catalog-service.dto.response.CupomValidacaoResponse.
@Data
public class CupomValidacaoDto {
    private boolean valido;
    private String motivoInvalido;
    private Long cupomId;
    private String codigo;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
}
