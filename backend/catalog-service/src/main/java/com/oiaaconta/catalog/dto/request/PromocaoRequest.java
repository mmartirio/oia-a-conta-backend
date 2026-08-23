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
public class PromocaoRequest {
    @NotBlank(message = "Nome da promoção obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "Tipo de desconto obrigatório")
    private TipoDesconto tipoDesconto;

    @NotNull(message = "Valor do desconto obrigatório")
    @DecimalMin(value = "0.01", message = "Valor do desconto deve ser maior que zero")
    private BigDecimal valorDesconto;

    @NotNull(message = "Alvo da promoção obrigatório")
    private TipoAlvo tipoAlvo; // TODOS ou GRUPO (INDIVIDUAL não é permitido em promoção)

    // Obrigatório sse tipoAlvo=GRUPO.
    private Long grupoClienteId;

    // Requisito extra opcional, checado em cima do gasto histórico do cliente
    // (calculado pelo order-service, que é quem tem esse dado).
    private BigDecimal requisitoGastoMinimo;

    @NotNull(message = "Data de início da validade obrigatória")
    private LocalDate validoDe;

    @NotNull(message = "Data de fim da validade obrigatória")
    private LocalDate validoAte;
}
