package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StatusFuncionamentoResponse {
    private boolean aberto;
    private String motivo;
    private LocalDateTime reaberturaPrevista;
}
