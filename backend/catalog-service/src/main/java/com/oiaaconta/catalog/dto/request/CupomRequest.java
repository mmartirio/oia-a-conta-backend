package com.oiaaconta.catalog.dto.request;

import com.oiaaconta.catalog.enums.TipoAlvo;
import com.oiaaconta.catalog.enums.TipoDesconto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CupomRequest {
    @NotBlank(message = "Código do cupom obrigatório")
    private String codigo;

    @NotNull(message = "Tipo de desconto obrigatório")
    private TipoDesconto tipoDesconto;

    @NotNull(message = "Valor do desconto obrigatório")
    @DecimalMin(value = "0.01", message = "Valor do desconto deve ser maior que zero")
    private BigDecimal valorDesconto;

    @NotNull(message = "Alvo do cupom obrigatório")
    private TipoAlvo tipoAlvo;

    // Obrigatório sse tipoAlvo=GRUPO.
    private Long grupoClienteId;

    // Obrigatório sse tipoAlvo=INDIVIDUAL.
    private Long clienteId;

    @NotNull(message = "Data de início da validade obrigatória")
    private LocalDate validoDe;

    @NotNull(message = "Data de fim da validade obrigatória")
    private LocalDate validoAte;
}
