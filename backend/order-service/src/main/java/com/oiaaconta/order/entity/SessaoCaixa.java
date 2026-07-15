package com.oiaaconta.order.entity;

import com.oiaaconta.order.enums.StatusSessaoCaixa;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes_caixa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoCaixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "aberto_por_id", nullable = false)
    private Long abertoPorId;

    @Column(name = "aberto_por_nome", nullable = false, length = 100)
    private String abertoPorNome;

    @Column(name = "valor_abertura", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAbertura;

    @CreationTimestamp
    @Column(name = "aberto_em", updatable = false)
    private LocalDateTime abertoEm;

    @Column(name = "fechado_por_id")
    private Long fechadoPorId;

    @Column(name = "fechado_por_nome", length = 100)
    private String fechadoPorNome;

    @Column(name = "valor_fechamento", precision = 10, scale = 2)
    private BigDecimal valorFechamento;

    @Column(name = "fechado_em")
    private LocalDateTime fechadoEm;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 10)
    private StatusSessaoCaixa status = StatusSessaoCaixa.ABERTO;
}
