package com.oiaaconta.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificacaoMensagemWhatsapp {
    private Long restauranteId;
    private String telefone;
    private String direcao;  // "ENVIADA" | "RECEBIDA"
    private String texto;
    private String criadoEm; // ISO-8601, serializado como String para não acoplar formato entre serviços
}
