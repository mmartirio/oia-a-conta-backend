package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Item individual de venda (comanda fechada ou entrega paga) dentro da sessão
// de caixa aberta — usado na Fila de Pagamento pra mostrar o que já foi
// vendido/recebido, complementando o resumo agregado (CaixaService.resumo).
// "descricao" é o rótulo curto da linha (mesa, quando houver, senão os
// produtos) — itensDescricao/troco/parcelas/bandeiraCartao só alimentam o
// modal de Detalhes.
@Data
@Builder
public class VendaResponse {
    private Long id;
    private String tipo; // COMANDA ou ENTREGA
    private LocalDateTime horario;
    private BigDecimal total;
    private String metodoPagamento;
    private String descricao;

    // Só preenchido quando a comanda tem mesa real cadastrada (mesaId > 0) —
    // usado pra exibir a identificação da mesa na linha, com o "descricao"
    // acima virando o rótulo curto (ex: "Mesa 5").
    private Integer mesaNumero;

    private String itensDescricao;
    private Integer parcelas;
    private String bandeiraCartao;
    private BigDecimal troco;
}
