package com.oiaaconta.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class GrupoResponse {
    private Long id;
    private String nome;
    private Set<String> permissoes;
    private long totalUsuarios;
    private boolean padrao;
    private LocalDateTime createdAt;
}
