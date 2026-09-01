package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// Cardápio numerado que o admin desenha pra imagem enviada pelo chatbot do
// WhatsApp — "numero" é o que o cliente digita no chat, resolvido de volta
// pro produtoId (ou comboId) real. Ver Produto.numeroCardapio /
// Combo.numeroCardapio. Exatamente um de produtoId/comboId vem preenchido —
// o outro fica null.
@Data
@Builder
public class ProdutoNumeradoResponse {
    private Integer numero;
    private Long produtoId;
    private Long comboId;
    private String nome;
    private BigDecimal preco;
}
