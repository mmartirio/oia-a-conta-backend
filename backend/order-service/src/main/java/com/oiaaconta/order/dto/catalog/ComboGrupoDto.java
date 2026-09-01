package com.oiaaconta.order.dto.catalog;

import lombok.Data;

import java.util.List;

// Espelha catalog-service.dto.response.ComboGrupoResponse.
@Data
public class ComboGrupoDto {
    private Long id;
    private String nome;
    private Integer quantidade;
    private List<ComboGrupoProdutoDto> produtos;
}
