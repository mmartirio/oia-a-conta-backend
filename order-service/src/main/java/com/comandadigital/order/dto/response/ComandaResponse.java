package com.comandadigital.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ComandaResponse {
    private Long id;
    private Long restauranteId;
    private Long mesaId;
    private Integer mesaNumero;
    private Long garconId;
    private String garconNome;
    private String status;
    private String metodoPagamento;
    private BigDecimal total;
    private List<PedidoResponse> pedidos;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
