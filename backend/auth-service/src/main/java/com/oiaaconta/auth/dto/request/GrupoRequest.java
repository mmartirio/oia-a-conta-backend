package com.oiaaconta.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class GrupoRequest {

    @NotBlank(message = "Nome obrigatório")
    private String nome;

    private Set<String> permissoes;
}
