package com.oiaaconta.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LogAuditoriaResponse {
    private Long id;
    private Long restauranteId;
    private String tipo;
    private String descricao;
    private Long usuarioId;
    private String usuarioNome;
    private LocalDateTime criadoEm;
}
