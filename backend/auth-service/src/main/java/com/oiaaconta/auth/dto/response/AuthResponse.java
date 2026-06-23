package com.oiaaconta.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UsuarioDto usuario;

    @Data
    @Builder
    @AllArgsConstructor
    public static class UsuarioDto {
        private Long id;
        private String nome;
        private String email;
        private String role;
        private Long restauranteId;
        private boolean ativo;
    }
}
