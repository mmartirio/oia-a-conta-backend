package com.comandadigital.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PedidoResponse {
    private Long id;
    private Long comandaId;
    private Long restauranteId;
    private Integer mesaNumero;
    private Long garconId;
    private String garconNome;
    private String status;
    private String observacao;
    private List<ItemPedidoResponse> itens;
    private LocalDateTime createdAt;
    private LocalDateTime readyAt;
}
