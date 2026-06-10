package com.comandadigital.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RestauranteResponse {
    private Long id;
    private String nome;
    private String slug;
    private String plano;
    private boolean ativo;
    private LocalDateTime createdAt;
}
