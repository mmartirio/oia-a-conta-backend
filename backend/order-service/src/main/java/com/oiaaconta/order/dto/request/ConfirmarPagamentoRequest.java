package com.oiaaconta.order.dto.request;

import com.oiaaconta.order.enums.MetodoPagamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConfirmarPagamentoRequest {
    @NotNull(message = "Método de pagamento obrigatório")
    private MetodoPagamento metodoPagamento;

    @Min(value = 1, message = "Mínimo de 1 parcela")
    @Max(value = 12, message = "Máximo de 12 parcelas")
    private Integer parcelas;
}
