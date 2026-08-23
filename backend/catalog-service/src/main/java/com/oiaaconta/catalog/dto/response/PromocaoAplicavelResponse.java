package com.oiaaconta.catalog.dto.response;

import com.oiaaconta.catalog.enums.TipoDesconto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromocaoAplicavelResponse {
    private Long promocaoId;
    private String nome;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
}
