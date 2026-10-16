package com.oiaaconta.ifood.dto.ifood;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IfoodCategoriaRequest {
    private String name;
    // AVAILABLE | UNAVAILABLE
    private String status;
    // DEFAULT (itens comuns) | PIZZA (com grupos de complemento obrigatórios)
    private String template;
    private Integer sequence;
}
