package com.oiaaconta.whatsapp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PedidoPublicoRequest {
    private String clienteNome;
    private String telefone;
    private String endereco;
    private String metodoPagamento;
    private String observacao;
    private List<ItemDto> itens;

    @Data
    public static class ItemDto {
        // Obrigatório sse comboId não for informado.
        private Long produtoId;
        // Nome de exibição — do produto OU do combo (frontend manda o nome do
        // combo aqui quando comboId está preenchido).
        private String produtoNome;
        private BigDecimal precoUnitario;
        private Integer quantidade;

        private Long comboId;
        private Integer comboQuantidade;
    }
}
