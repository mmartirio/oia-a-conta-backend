package com.oiaaconta.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMensal;

    @Column(name = "limite_usuarios")
    @Builder.Default
    private Integer limiteUsuarios = 10;

    @Column(name = "limite_mesas")
    @Builder.Default
    private Integer limiteMesas = 20;

    @Column(name = "funcionalidades", columnDefinition = "TEXT")
    private String funcionalidades;

    @Column(name = "periodo_teste")
    @Builder.Default
    private Boolean periodoTeste = false;

    @Column(name = "dias_teste")
    @Builder.Default
    private Integer diasTeste = 30;

    @Builder.Default
    private boolean ativo = true;

    @Builder.Default
    private boolean destaque = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
