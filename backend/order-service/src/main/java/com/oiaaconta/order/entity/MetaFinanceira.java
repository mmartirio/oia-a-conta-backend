package com.oiaaconta.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Metas do mês do restaurante (aba Metas do Financeiro). Campos nulos = sem
// meta definida pra aquele indicador.
@Entity
@Table(name = "metas_financeiras")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    // "2026-10"
    @Column(nullable = false, length = 7)
    private String mes;

    @Column(name = "meta_faturamento", precision = 12, scale = 2)
    private BigDecimal metaFaturamento;

    @Column(name = "meta_lucro", precision = 12, scale = 2)
    private BigDecimal metaLucro;

    @Column(name = "limite_despesas", precision = 12, scale = 2)
    private BigDecimal limiteDespesas;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
