package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    // ID token bruto (JWT) retornado pelo Google Identity Services no
    // frontend — verificado (assinatura + audience) no backend antes de
    // qualquer decisão de login, nunca confiado em texto puro.
    @NotBlank(message = "Credential do Google obrigatório")
    private String credential;
}
