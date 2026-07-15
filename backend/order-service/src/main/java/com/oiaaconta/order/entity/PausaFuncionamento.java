package com.oiaaconta.order.entity;

import com.oiaaconta.order.enums.TipoPausa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pausas_funcionamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PausaFuncionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPausa tipo;

    @Column(length = 200)
    private String titulo;

    @Column(length = 500)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
