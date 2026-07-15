package com.oiaaconta.catalog.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProdutoRequest {
    @NotBlank(message = "Nome do produto obrigatório")
    private String nome;

    private String descricao;

    @NotNull(message = "Preço obrigatório")
    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    private BigDecimal preco;

    @NotNull(message = "Categoria obrigatória")
    private Long categoriaId;

    // Foto ilustrativa: data URI completa (ex: "data:image/png;base64,...").
    // null = não alterar (update) / sem foto (create); "" explícito = remover a foto atual.
    private String imagemBase64;
}
