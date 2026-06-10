package com.comandadigital.table.entity;

import com.comandadigital.table.enums.StatusMesa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mesas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"restaurante_id", "numero"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false)
    private Integer numero;

    @Builder.Default
    private Integer capacidade = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusMesa status = StatusMesa.DISPONIVEL;
}
