package com.oiaaconta.ifood.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Correlação entre uma entidade local (categoria/produto/combo) e o item
// correspondente criado no catálogo do iFood — necessária nos dois sentidos:
// pra saber se sincroniza um PUT (já existe lá) ou POST (cria), e pra
// traduzir um item de pedido do iFood de volta pro nosso produtoId/comboId
// na hora de montar a Entrega (ver IfoodEventsPoller).
@Entity
@Table(name = "ifood_mapeamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IfoodMapeamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    // CATEGORIA | PRODUTO | COMBO
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    @Column(name = "ifood_id", nullable = false, length = 100)
    private String ifoodId;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
