package com.oiaaconta.catalog.dto.response;

import com.oiaaconta.catalog.enums.TipoAlvo;
import com.oiaaconta.catalog.enums.TipoDesconto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CupomResponse {
    private Long id;
    private Long restauranteId;
    private String codigo;
    private TipoDesconto tipoDesconto;
    private BigDecimal valorDesconto;
    private TipoAlvo tipoAlvo;
    private Long grupoClienteId;
    private String grupoClienteNome;
    private Long clienteId;
    private String clienteNome;
    private LocalDate validoDe;
    private LocalDate validoAte;
    private boolean ativo;
}
