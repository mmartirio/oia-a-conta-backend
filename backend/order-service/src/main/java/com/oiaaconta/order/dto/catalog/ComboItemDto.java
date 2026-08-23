package com.oiaaconta.order.dto.catalog;

import lombok.Data;

import java.math.BigDecimal;

// Espelha catalog-service.dto.response.ComboItemResponse.
@Data
public class ComboItemDto {
    private Long produtoId;
    private String produtoNome;
    private int quantidade;
    private BigDecimal valorAlocado;
}
