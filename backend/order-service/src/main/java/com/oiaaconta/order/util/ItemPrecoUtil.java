package com.oiaaconta.order.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Subtotal de uma linha de pedido/entrega — centraliza a única diferença
// entre item por UNIDADE, PESO e VOLUME: o multiplicador aplicado sobre
// precoUnitario. Por UNIDADE é a contagem (quantidade); por PESO é o peso
// vendido (pesoGramas) convertido pra quilos; por VOLUME é o volume vendido
// (volumeMililitros) convertido pra litros — o preço do produto é sempre
// "por kg"/"por litro" nesses casos (ver Produto.tipoPreco, catalog-service).
// Um item nunca tem os dois (pesoGramas e volumeMililitros) preenchidos.
public final class ItemPrecoUtil {

    private static final BigDecimal UNIDADES_POR_MIL = BigDecimal.valueOf(1000);

    private ItemPrecoUtil() {}

    public static BigDecimal subtotal(BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoGramas) {
        return subtotal(precoUnitario, quantidade, pesoGramas, null);
    }

    public static BigDecimal subtotal(BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoGramas, BigDecimal volumeMililitros) {
        BigDecimal fracao = pesoGramas != null ? pesoGramas : volumeMililitros;
        BigDecimal multiplicador = fracao != null
            ? fracao.divide(UNIDADES_POR_MIL, 6, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(quantidade);
        return precoUnitario.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);
    }
}
