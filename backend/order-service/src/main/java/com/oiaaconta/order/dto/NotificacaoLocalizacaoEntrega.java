package com.oiaaconta.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificacaoLocalizacaoEntrega {
    private Long restauranteId;
    private Long entregaId;
    private String entregadorNome;
    private Double latitude;
    private Double longitude;
    private String atualizadoEm; // ISO-8601, serializado como String para não acoplar formato entre serviços
}
