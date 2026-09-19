package com.oiaaconta.billing.entity;

import com.oiaaconta.billing.enums.ModalidadeOperacao;
import com.oiaaconta.billing.enums.StatusContrato;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contratos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false, unique = true)
    private Long restauranteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusContrato status = StatusContrato.TRIAL;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "data_proximo_vencimento")
    private LocalDate dataProximoVencimento;

    @Column(name = "mp_preapproval_id", length = 100)
    private String mpPreapprovalId;

    // Só preenchida quando Plano.exigeModalidadeOperacao é true (ex: plano
    // Startup) — nos demais planos fica null e é ignorada (mesas e delivery
    // liberados sem restrição).
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_operacao", length = 20)
    private ModalidadeOperacao modalidadeOperacao;

    @Column(name = "dias_carencia")
    @Builder.Default
    private Integer diasCarencia = 5;

    // Trocas gratuitas de modalidade usadas (máximo 2 — ver
    // BillingService.LIMITE_TROCAS_GRATIS). Da 3ª em diante, só o suporte
    // pode trocar, e cada troca soma VALOR_ENCARGO_TROCA em
    // saldoEncargosModalidade.
    @Column(name = "trocas_modalidade_gratis_usadas", nullable = false)
    @Builder.Default
    private int trocasModalidadeGratisUsadas = 0;

    // Encargos pendentes de trocas de modalidade além do limite gratuito —
    // somado automaticamente no valor do próximo pagamento manual
    // registrado pra esse contrato (ver BillingService.registrarPagamentoManual)
    // e zerado depois.
    @Column(name = "saldo_encargos_modalidade", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal saldoEncargosModalidade = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
