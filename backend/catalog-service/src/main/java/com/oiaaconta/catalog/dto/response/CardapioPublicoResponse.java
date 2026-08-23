package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CardapioPublicoResponse {
    private List<CardapioCategoriaResponse> categorias;
    private List<ComboResponse> combos;

    // Selo de promoção pública: só considera promoções com alvo TODOS (cliente
    // anônimo não tem grupo/histórico pra elegibilidade de GRUPO) e sem
    // requisito de gasto — mostrado igual em todos os produtos, já que a
    // promoção é do pedido inteiro, não por item.
    private boolean promocaoAtiva;
    private String promocaoDescricao;
    private BigDecimal promocaoValorDesconto;
    private String promocaoTipoDesconto;
}
