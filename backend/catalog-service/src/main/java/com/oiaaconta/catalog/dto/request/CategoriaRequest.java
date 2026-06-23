package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank(message = "Nome da categoria obrigatório")
    private String nome;
}
