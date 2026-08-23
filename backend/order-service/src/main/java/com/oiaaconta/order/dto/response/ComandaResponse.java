package com.oiaaconta.order.dto.response;

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
    private Integer parcelas;
    // subtotal = soma dos itens, sem desconto; total = subtotal - desconto (valor a pagar).
    private BigDecimal subtotal;
    private BigDecimal desconto;
    private BigDecimal total;
    private Long clienteId;
    private String descontoTipo;
    private String descontoOrigemDescricao;
    private List<PedidoResponse> pedidos;
    private LocalDateTime criadoEm;
    private LocalDateTime fechadoEm;
    private LocalDateTime aguardandoPagamentoEm;
}
