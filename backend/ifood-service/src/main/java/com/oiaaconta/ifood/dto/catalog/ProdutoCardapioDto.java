package com.oiaaconta.ifood.dto.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

// CardapioService (catalog-service) já filtra só categorias/produtos ativos
// — o que não aparece aqui está inativo. Sem campo "ativo": ausência do
// item na resposta É o sinal de indisponibilidade (ver
// IfoodCatalogSyncService, que marca UNAVAILABLE no iFood o que não foi
// tocado numa rodada de sincronização).
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProdutoCardapioDto {
    private Long id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
}
