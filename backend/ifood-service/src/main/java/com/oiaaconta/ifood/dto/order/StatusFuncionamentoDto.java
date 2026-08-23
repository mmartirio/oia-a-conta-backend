package com.oiaaconta.ifood.dto.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Espelha StatusFuncionamentoResponse do order-service
// (GET /api/configuracoes/pausas/status?restauranteId=, já público) — é a
// mesma verificação (manual + pausa programada + horário semanal) usada
// pro painel do admin, aqui reaproveitada pra decidir se abre/fecha a loja
// no iFood.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusFuncionamentoDto {
    private boolean aberto;
    private String motivo;
}
