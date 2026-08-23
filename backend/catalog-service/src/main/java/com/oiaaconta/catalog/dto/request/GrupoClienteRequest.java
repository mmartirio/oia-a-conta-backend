package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GrupoClienteRequest {
    @NotBlank(message = "Nome do grupo obrigatório")
    private String nome;

    private String descricao;
}
