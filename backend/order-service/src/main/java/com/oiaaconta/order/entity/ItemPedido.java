package com.oiaaconta.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(name = "produto_nome", nullable = false, length = 200)
    private String produtoNome;

    // Pra produto por UNIDADE: contagem (sempre usada). Pra produto por PESO:
    // fica fixo em 1 (cada linha = uma pesagem) e o peso vendido vai em
    // pesoGramas — ver ItemPrecoUtil.subtotal().
    @Column(nullable = false)
    private Integer quantidade;

    // Preenchido só quando o produto é vendido por peso (Produto.tipoPreco
    // PESO no catalog-service) — peso vendido já normalizado em gramas,
    // independente da unidade (mg/g/kg) usada na hora de digitar.
    @Column(name = "peso_gramas", precision = 10, scale = 3)
    private BigDecimal pesoGramas;

    // Preenchido só quando o produto é vendido por volume (Produto.tipoPreco
    // VOLUME) — volume vendido já normalizado em mililitros, independente da
    // unidade (ml/litro) usada na hora de digitar. Nunca preenchido junto
    // com pesoGramas — um item é por unidade, peso OU volume.
    @Column(name = "volume_mililitros", precision = 10, scale = 3)
    private BigDecimal volumeMililitros;

    private String observacao;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    // Preenchido quando este item veio da expansão de um Combo — produtoId
    // continua sendo o produto real (estoque/cozinha não mudam), isto é só
    // pra agrupar visualmente no recibo/KDS de onde a linha veio.
    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "combo_nome", length = 150)
    private String comboNome;
}
