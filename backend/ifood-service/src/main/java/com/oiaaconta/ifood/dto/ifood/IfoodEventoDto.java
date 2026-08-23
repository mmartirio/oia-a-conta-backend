package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Item de GET /events/v1.0/events:polling — code "PLC" é pedido colocado
// (o único tipo que processamos por enquanto); outros códigos (CFM, CAN,
// DSP etc.) chegam junto no mesmo polling e são só reconhecidos/ignorados.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodEventoDto {
    private String id;
    private String code;
    private String orderId;
}
