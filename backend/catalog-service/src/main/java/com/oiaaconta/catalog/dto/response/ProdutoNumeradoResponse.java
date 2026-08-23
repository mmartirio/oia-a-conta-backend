package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// Cardápio numerado que o admin desenha pra imagem enviada pelo chatbot do
// WhatsApp — "numero" é o que o cliente digita no chat, resolvido de volta
// pro produtoId real. Ver Produto.numeroCardapio.
@Data
@Builder
public class ProdutoNumeradoResponse {
    private Integer numero;
    private Long produtoId;
    private String nome;
    private BigDecimal preco;
}
