package com.oiaaconta.ifood.dto.ifood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfoodInterrupcaoRequest {
    // ISO-8601, ambos obrigatórios pela API do iFood — usamos uma janela
    // longa (24h) já que a reabertura de verdade acontece removendo a
    // interrupção antes do fim, não esperando o prazo.
    private String start;
    private String end;
    private String description;
}
