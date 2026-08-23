package com.oiaaconta.ifood.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoriaCardapioDto {
    private Long id;
    private String nome;
    private List<ProdutoCardapioDto> produtos;
}
