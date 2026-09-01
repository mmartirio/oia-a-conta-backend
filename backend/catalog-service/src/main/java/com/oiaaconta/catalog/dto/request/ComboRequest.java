package com.oiaaconta.catalog.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    // Número pro cliente pedir esse combo no chat do WhatsApp — null = combo
    // não entra no cardápio numerado (só disponível pelo cardápio público).
    private Integer numeroCardapio;

    // Ex: "2 Pastéis" (quantidade=2, produtoIds=[carne,frango,...]) — o
    // cliente escolhe quais sabores quer dentro de cada grupo, sem alterar o
    // preço do combo (fixo, definido acima).
    @NotEmpty(message = "O combo precisa de pelo menos um grupo (ex: \"2 Pastéis\")")
    @Valid
    private List<ComboGrupoRequest> grupos;
}
