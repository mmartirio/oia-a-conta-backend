package com.oiaaconta.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardapioCategoriaResponse {
    private Long id;
    private String nome;
    private List<ProdutoResponse> produtos;
}
