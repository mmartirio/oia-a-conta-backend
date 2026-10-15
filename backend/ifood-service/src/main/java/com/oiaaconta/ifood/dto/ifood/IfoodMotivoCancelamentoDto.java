package com.oiaaconta.ifood.dto.ifood;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Item de GET /order/v1.0/orders/{id}/cancellationReasons — a API só aceita
// em requestCancellation um código válido para o status atual do pedido
// (varia por pedido), por isso não dá pra usar um código fixo: é obrigatório
// consultar essa lista antes de cada cancelamento (ver IfoodInternalController).
// @JsonAlias cobre "code" e "cancelCodeId" (variações vistas na doc do iFood
// pra esse mesmo campo, dependendo da versão/módulo).
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IfoodMotivoCancelamentoDto {
    @JsonAlias({"code", "cancelCodeId"})
    private String cancelCodeId;
    private String description;
}
