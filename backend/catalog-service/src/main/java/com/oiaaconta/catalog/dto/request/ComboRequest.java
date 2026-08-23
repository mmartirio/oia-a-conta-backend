package com.oiaaconta.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ComboRequest {
    @NotBlank(message = "Nome do combo obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "Preço obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    // null = não altera (update) / sem foto (create); "" explícito = remover a foto atual.
    private String imagemBase64;

    @NotNull(message = "Itens do combo obrigatórios")
    @Size(min = 2, message = "Um combo precisa de pelo menos 2 produtos distintos")
    @Valid
    private List<ItemQuantidadeRequest> itens;
}
