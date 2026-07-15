package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EncerramentoAntecipadoRequest {
    @NotBlank(message = "Motivo obrigatório")
    private String motivo;
}
