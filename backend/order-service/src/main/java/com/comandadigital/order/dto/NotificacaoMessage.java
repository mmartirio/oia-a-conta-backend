package com.comandadigital.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificacaoMessage {
    private String tipo;
    private Long pedidoId;
    private Long comandaId;
    private Long restauranteId;
    private Long garconId;
    private String garconNome;
    private Integer mesaNumero;
    private String mensagem;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
