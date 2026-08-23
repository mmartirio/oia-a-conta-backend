package com.oiaaconta.catalog.enums;

// Promocao não usa INDIVIDUAL (rejeitado na validação do PromocaoService) — só
// Cupom permite alvo de cliente único.
public enum TipoAlvo {
    TODOS, GRUPO, INDIVIDUAL
}
