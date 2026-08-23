package com.oiaaconta.ifood.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComboCardapioDto {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
}
