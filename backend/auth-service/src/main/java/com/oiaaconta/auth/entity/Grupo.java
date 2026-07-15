package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "grupos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 100)
    private String nome;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "grupo_permissoes", joinColumns = @JoinColumn(name = "grupo_id"))
    @Column(name = "permissao", length = 40)
    @Builder.Default
    private Set<String> permissoes = new HashSet<>();

    // Grupos padrão (Administrador/Garçom/Cozinheiro/Entregador) vêm
    // pré-criados pra cada restaurante e não podem ser excluídos — só
    // editados (nome/permissões), se o admin quiser ajustar.
    @Builder.Default
    private boolean padrao = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
