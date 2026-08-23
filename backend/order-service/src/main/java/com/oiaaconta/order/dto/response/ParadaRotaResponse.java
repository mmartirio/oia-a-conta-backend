package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParadaRotaResponse {
    private Long entregaId;
    private int ordem;
    private String clienteNome;
    private String enderecoResumo;
    // Distância/tempo desde a parada anterior (ou da posição atual do
    // entregador, na primeira parada) até esta.
    private BigDecimal distanciaKm;
    private BigDecimal duracaoMin;
    // Pedido priorizado por tempo de espera, não pela otimização de rota do
    // OSRM — ver RotaService.
    private boolean urgente;
}
