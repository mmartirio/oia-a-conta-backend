package com.oiaaconta.billing.dto.response;

import com.oiaaconta.billing.enums.ModalidadeOperacao;

// Consultado por table-service, order-service, ifood-service e
// auth-service — cada um usa só os campos que precisa. limiteMesas e
// limiteAtendentesWhatsapp: null = sem limite.
public record RestricoesOperacaoResponse(
    boolean restringeModalidade,
    ModalidadeOperacao modalidadeOperacao,
    Integer limiteMesas,
    Integer limiteAtendentesWhatsapp,
    boolean permiteIfood
) {
}
