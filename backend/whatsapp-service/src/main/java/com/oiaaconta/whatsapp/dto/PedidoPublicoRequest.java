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
        private Long produtoId;
        private String produtoNome;
        private BigDecimal precoUnitario;
        private Integer quantidade;
    }
}
