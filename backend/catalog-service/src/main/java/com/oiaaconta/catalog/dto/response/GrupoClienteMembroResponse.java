package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GrupoClienteMembroResponse {
    private Long clienteId;
    private String clienteNome;
    private String clienteTelefone;
    private LocalDateTime adicionadoEm;
}
