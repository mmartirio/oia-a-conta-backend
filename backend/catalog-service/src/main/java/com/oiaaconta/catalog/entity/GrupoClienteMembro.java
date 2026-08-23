package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "grupo_cliente_membros")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupoClienteMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "grupo_cliente_id", nullable = false)
    private Long grupoClienteId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @CreationTimestamp
    @Column(name = "adicionado_em", updatable = false)
    private LocalDateTime adicionadoEm;
}
