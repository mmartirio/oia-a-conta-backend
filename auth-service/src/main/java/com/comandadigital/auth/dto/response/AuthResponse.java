package com.comandadigital.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String nome;
    private String email;
    private String role;
    private Long restauranteId;
    private String restauranteNome;
}
