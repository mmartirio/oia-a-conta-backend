package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RotaSugeridaResponse {
    private List<ParadaRotaResponse> paradas;
    private BigDecimal distanciaTotalKm;
    private BigDecimal duracaoTotalMin;
    // Entregas ativas que não entraram na rota por falta de coordenadas
    // (endereço não geocodificado) — o frontend ainda mostra esses cards,
    // só sem selo de ordem.
    private List<Long> semCoordenadas;
}
