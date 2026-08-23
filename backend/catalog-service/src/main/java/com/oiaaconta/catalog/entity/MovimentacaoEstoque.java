package com.oiaaconta.catalog.entity;

import com.oiaaconta.catalog.enums.TipoMovimentacaoEstoque;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "quantidade_resultante", nullable = false)
    private int quantidadeResultante;

    @Column(length = 300)
    private String motivo;

    @Column(name = "referencia_externa", length = 50)
    private String referenciaExterna;

    @Column(name = "criado_por_id")
    private Long criadoPorId;

    @Column(name = "criado_por_nome", length = 100)
    private String criadoPorNome;

    @CreationTimestamp
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;
}
