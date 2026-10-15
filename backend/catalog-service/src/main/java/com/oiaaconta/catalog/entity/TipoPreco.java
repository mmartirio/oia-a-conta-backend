package com.oiaaconta.catalog.entity;

// UNIDADE: preco é por unidade, quantidade no pedido é uma contagem inteira.
// PESO: preco é por quilo, quantidade no pedido é um peso (ver
// order-service — o peso é digitado em mg/g/kg e convertido pra gramas).
// VOLUME: preco é por litro, quantidade no pedido é um volume (digitado em
// ml ou litro e suas fracões, convertido pra mililitros — mesma ideia de
// PESO, ver order-service ItemPedido.volumeMililitros).
public enum TipoPreco {
    UNIDADE,
    PESO,
    VOLUME
}
