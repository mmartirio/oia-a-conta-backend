package com.oiaaconta.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String nome;
    private String email;
    private String role;
    private boolean ativo;
    private Long restauranteId;
    private LocalDateTime createdAt;
    private Long grupoId;
    private String grupoNome;
    private Set<String> permissoes;
    private boolean donoConta;
}
