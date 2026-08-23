package com.oiaaconta.ifood.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// Espelha só os campos usados aqui do CardapioPublicoResponse do
// catalog-service (GET /api/catalog/publico/{restauranteId}/cardapio,
// endpoint já público, sem autenticação) — ignora o resto (promoção etc.).
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardapioPublicoDto {
    private List<CategoriaCardapioDto> categorias;
    private List<ComboCardapioDto> combos;
}
