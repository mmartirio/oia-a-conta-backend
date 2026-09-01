package com.oiaaconta.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ComboGrupoResponse {
    private Long id;
    private String nome;
    private Integer quantidade;
    private List<ComboGrupoProdutoResponse> produtos;
}
