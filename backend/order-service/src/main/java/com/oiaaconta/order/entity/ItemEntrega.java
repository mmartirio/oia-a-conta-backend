package com.oiaaconta.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(name = "produto_nome", nullable = false, length = 200)
    private String produtoNome;

    @Column(nullable = false)
    private Integer quantidade;

    private String observacao;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    // Preenchido quando este item veio da expansão de um Combo — produtoId
    // continua sendo o produto real; isto é só pra agrupar visualmente no
    // recibo/KDS de onde a linha veio (espelha ItemPedido.comboId/comboNome).
    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "combo_nome", length = 150)
    private String comboNome;
}
