package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "combos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 300)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "imagem_base64", columnDefinition = "TEXT")
    private String imagemBase64;

    // Número que o cliente digita no chat do WhatsApp pra pedir esse combo —
    // mesmo espaço de numeração de Produto.numeroCardapio (ver
    // ProdutoNumeradoResponse), null = combo não aparece no cardápio numerado.
    @Column(name = "numero_cardapio")
    private Integer numeroCardapio;

    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
