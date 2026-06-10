package com.comandadigital.order.dto.request;

import com.comandadigital.order.enums.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FecharComandaRequest {
    @NotNull(message = "Método de pagamento obrigatório")
    private MetodoPagamento metodoPagamento;
}
