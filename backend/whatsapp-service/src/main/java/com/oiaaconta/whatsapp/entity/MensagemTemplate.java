package com.oiaaconta.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mensagem_template",
    uniqueConstraints = @UniqueConstraint(columnNames = {"restaurante_id", "chave"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MensagemTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "label", length = 150)
    private String label;

    @Column(name = "grupo", length = 30)
    private String grupo;

    @Column(name = "ordem")
    private Integer ordem;

    @Column(name = "ativo")
    private Boolean ativo; // null ou true = ativo; false = desativado pelo usuário
}
