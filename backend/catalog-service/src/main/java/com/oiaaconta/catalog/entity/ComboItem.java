package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "combo_itens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "combo_id", nullable = false)
    private Long comboId;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Builder.Default
    @Column(nullable = false)
    private int quantidade = 1;
}
