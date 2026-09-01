package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class ComboGrupoRequest {
    @NotBlank(message = "Nome do grupo obrigatório")
    private String nome;

    @NotNull(message = "Quantidade do grupo obrigatória")
    @Min(value = 1, message = "Quantidade do grupo deve ser pelo menos 1")
    private Integer quantidade;

    @NotEmpty(message = "Selecione pelo menos um produto elegível pro grupo")
    private List<Long> produtoIds;
}
