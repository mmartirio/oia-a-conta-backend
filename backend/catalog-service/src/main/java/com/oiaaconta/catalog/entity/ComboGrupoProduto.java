package com.oiaaconta.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

// Um produto elegível dentro de um grupo do combo (ver ComboGrupo) — o
// cliente escolhe entre esses ao montar o pedido.
@Entity
@Table(name = "combo_grupo_produtos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboGrupoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grupo_id", nullable = false)
    private Long grupoId;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;
}
