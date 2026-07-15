package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PausaResponse {
    private Long id;
    private Long restauranteId;
    private String tipo;
    private String titulo;
    private String motivo;
    private LocalDateTime inicio;
    private LocalDateTime fim;
}
