package com.oiaaconta.auth.dto.request;

import lombok.Data;

@Data
public class BackgroundRequest {
    // Data URI completa (ex: "data:image/png;base64,...") ou null/vazio para remover o fundo.
    private String backgroundBase64;
    private Integer backgroundOpacidade;
}
