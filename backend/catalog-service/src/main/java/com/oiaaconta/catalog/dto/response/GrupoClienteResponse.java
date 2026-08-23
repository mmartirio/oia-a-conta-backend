package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GrupoClienteResponse {
    private Long id;
    private Long restauranteId;
    private String nome;
    private String descricao;
    private boolean ativo;
    private long totalMembros;
    private LocalDateTime createdAt;
}
