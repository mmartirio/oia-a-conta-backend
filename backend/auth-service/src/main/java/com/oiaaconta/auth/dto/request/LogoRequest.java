package com.oiaaconta.auth.dto.request;

import lombok.Data;

@Data
public class LogoRequest {
    // Data URI completa (ex: "data:image/png;base64,...") ou null/vazio para remover a logo.
    private String logoBase64;
}
