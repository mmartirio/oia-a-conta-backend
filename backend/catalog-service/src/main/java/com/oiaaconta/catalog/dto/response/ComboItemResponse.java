package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComboItemResponse {
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    // Valor total (já considerando a quantidade desta linha) alocado a este
    // produto dentro do preço fixo do combo — preço do combo rateado
    // proporcionalmente ao preço de tabela de cada produto. Usado pelo
    // order-service pra expandir o combo em itens de pedido reais sem duplicar
    // a lógica de rateio; dividir por quantidade pra obter o preço unitário.
    private BigDecimal valorAlocado;
}
