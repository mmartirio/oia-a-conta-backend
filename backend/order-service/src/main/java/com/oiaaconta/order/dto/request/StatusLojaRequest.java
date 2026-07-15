package com.oiaaconta.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusLojaRequest {

    @NotNull(message = "Campo fechado obrigatório")
    private Boolean fechado;

    private String motivo;
}
