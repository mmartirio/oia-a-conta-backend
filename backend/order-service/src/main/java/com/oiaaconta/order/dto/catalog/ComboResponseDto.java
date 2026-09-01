package com.oiaaconta.order.dto.catalog;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Espelha catalog-service.dto.response.ComboResponse.
@Data
public class ComboResponseDto {
    private Long id;
    private String nome;
    private BigDecimal preco;
    private boolean ativo;
    private List<ComboGrupoDto> grupos;
}
