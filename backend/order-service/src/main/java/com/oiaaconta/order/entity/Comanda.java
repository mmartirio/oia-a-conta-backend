package com.oiaaconta.order.entity;

import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.enums.StatusComanda;
import com.oiaaconta.order.enums.TipoDescontoOrigem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comandas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "mesa_id", nullable = false)
    private Long mesaId;

    @Column(name = "mesa_numero", nullable = false)
    private Integer mesaNumero;

    @Column(name = "garcon_id", nullable = false)
    private Long garconId;

    @Column(name = "garcon_nome", nullable = false, length = 100)
    private String garconNome;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusComanda status = StatusComanda.ABERTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private MetodoPagamento metodoPagamento;

    private Integer parcelas;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Marca quando a comanda entrou em AGUARDANDO_PAGAMENTO — usado pra
    // ordenar a fila do caixa por ordem de chegada (não dá pra usar
    // createdAt, que é quando a mesa foi aberta, não quando ficou pronta
    // pra pagar).
    @Column(name = "aguardando_pagamento_em")
    private LocalDateTime aguardandoPagamentoEm;

    // Referência cross-service ao Cliente em catalog-service (sem FK, mesmo
    // padrão de restauranteId) — null quando a venda não identificou cliente.
    @Column(name = "cliente_id")
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "desconto_tipo", length = 10)
    private TipoDescontoOrigem descontoTipo;

    @Column(name = "desconto_origem_id")
    private Long descontoOrigemId;

    @Column(name = "desconto_origem_descricao", length = 150)
    private String descontoOrigemDescricao;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    // Snapshot persistido em confirmarPagamento (subtotal - desconto) —
    // evita recalcular a mesma soma em múltiplos lugares depois que a
    // comanda fecha.
    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;
}
