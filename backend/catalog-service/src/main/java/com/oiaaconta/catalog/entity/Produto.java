package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    // Foto ilustrativa do produto: data URI completa (ex: "data:image/jpeg;base64,...") ou null.
    @Column(name = "imagem_base64", columnDefinition = "TEXT")
    private String imagemBase64;

    // Número desse produto na imagem do cardápio numerado que o admin desenha
    // pra enviar no chatbot do WhatsApp — nulo pra produtos que não aparecem
    // nessa imagem. Não tem relação com a ordem de exibição do cardápio
    // público (essa é só por categoria.ordem).
    @Column(name = "numero_cardapio")
    private Integer numeroCardapio;

    @Builder.Default
    private boolean ativo = true;
}
