package com.comandadigital.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
}
