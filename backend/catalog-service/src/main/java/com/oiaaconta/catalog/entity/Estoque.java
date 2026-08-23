package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Builder.Default
    @Column(nullable = false)
    private int quantidade = 0;

    @Builder.Default
    @Column(name = "quantidade_minima", nullable = false)
    private int quantidadeMinima = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean controlado = false;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
