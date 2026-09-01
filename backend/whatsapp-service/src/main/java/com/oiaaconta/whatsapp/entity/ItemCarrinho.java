package com.oiaaconta.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_carrinho")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoWhatsapp sessao;

    // Nulo quando o item é um Combo (ver comboId abaixo).
    @Column(name = "produto_id")
    private Long produtoId;

    // Nome de exibição — do produto OU do combo, indistintamente (usado como
    // está no texto de confirmação do WhatsApp).
    @Column(name = "produto_nome", nullable = false, length = 200)
    private String produtoNome;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "combo_nome", length = 150)
    private String comboNome;

    // Sabores escolhidos pelo cliente dentro dos grupos do combo, se houver —
    // formato simples "produtoId:quantidade;produtoId:quantidade", acumulado
    // conforme cada grupo é respondido (ver ChatbotService). Null pra itens
    // que não são combo, ou combos sem grupos configurados (sem escolha).
    @Column(name = "sabores_escolhidos", columnDefinition = "TEXT")
    private String saboresEscolhidos;
}
