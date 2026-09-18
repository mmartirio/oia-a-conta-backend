package com.oiaaconta.ifood.dto.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

// Espelha com.oiaaconta.billing.dto.response.RestricoesOperacaoResponse
// (billing-service) — ifood-service só usa permiteIfood.
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestricoesOperacaoDto {
    private boolean permiteIfood;
}
