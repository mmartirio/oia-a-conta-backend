package com.oiaaconta.order.dto.catalog;

import com.oiaaconta.order.enums.TipoDesconto;
import lombok.Data;

import java.math.BigDecimal;

// Espelha catalog-service.dto.response.PromocaoAplicavelResponse.
@Data
public class PromocaoAplicavelDto {
    private Long promocaoId;
    private String nome;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
}
