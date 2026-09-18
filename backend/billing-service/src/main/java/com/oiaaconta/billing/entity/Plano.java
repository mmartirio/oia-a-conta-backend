package com.oiaaconta.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "preco_mensal", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMensal;

    @Column(name = "limite_usuarios")
    @Builder.Default
    private Integer limiteUsuarios = 10;

    @Column(name = "limite_mesas")
    @Builder.Default
    private Integer limiteMesas = 20;

    // Usada só quando o plano NÃO exige modalidade (ex: PRO). Planos que
    // exigem modalidade usam funcionalidadesMesas/funcionalidadesDelivery
    // abaixo, já que os recursos liberados diferem entre presencial e
    // delivery (ex: Start UP não tem "Controle de mesas" no delivery).
    @Column(name = "funcionalidades", columnDefinition = "TEXT")
    private String funcionalidades;

    @Column(name = "funcionalidades_mesas", columnDefinition = "TEXT")
    private String funcionalidadesMesas;

    @Column(name = "funcionalidades_delivery", columnDefinition = "TEXT")
    private String funcionalidadesDelivery;

    @Column(name = "periodo_teste")
    @Builder.Default
    private Boolean periodoTeste = false;

    @Column(name = "dias_teste")
    @Builder.Default
    private Integer diasTeste = 30;

    @Builder.Default
    private boolean ativo = true;

    @Builder.Default
    private boolean destaque = false;

    // Só planos como o Startup exigem que o restaurante escolha uma
    // modalidade de operação (mesas ou delivery) no cadastro — os demais
    // planos liberam ambas sem restrição. Ver Contrato.modalidadeOperacao.
    @Column(name = "exige_modalidade_operacao", nullable = false)
    @Builder.Default
    private boolean exigeModalidadeOperacao = false;

    // Limite de atendentes de WhatsApp (usuários com permissão de
    // conversas/mensagens) — qual dos três vale depende da modalidade do
    // contrato: se o plano não exige modalidade, vale limiteAtendentesWhatsapp;
    // se exige, vale a versão Mesas ou Delivery conforme a escolha do
    // restaurante. Null em qualquer um = sem limite. Ver
    // BillingService.buscarRestricoesOperacao, que resolve isso num valor só.
    @Column(name = "limite_atendentes_whatsapp")
    private Integer limiteAtendentesWhatsapp;

    @Column(name = "limite_atendentes_whatsapp_mesas")
    private Integer limiteAtendentesWhatsappMesas;

    @Column(name = "limite_atendentes_whatsapp_delivery")
    private Integer limiteAtendentesWhatsappDelivery;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
