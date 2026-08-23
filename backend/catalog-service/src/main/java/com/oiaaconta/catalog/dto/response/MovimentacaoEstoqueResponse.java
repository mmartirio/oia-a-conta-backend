package com.oiaaconta.catalog.dto.response;

import com.oiaaconta.catalog.enums.TipoMovimentacaoEstoque;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MovimentacaoEstoqueResponse {
    private Long id;
    private Long produtoId;
    private TipoMovimentacaoEstoque tipo;
    private int quantidade;
    private int quantidadeResultante;
    private String motivo;
    private String referenciaExterna;
    private String criadoPorNome;
    private LocalDateTime criadoEm;
}
