package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combo_grupos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "combo_id", nullable = false)
    private Long comboId;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordem = 0;
}
