package com.oiaaconta.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "restaurante_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestauranteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false, unique = true)
    private Long restauranteId;

    @Column(name = "pix_chave", length = 200)
    private String pixChave;

    @Builder.Default
    @Column(name = "comissao_garcon", precision = 5, scale = 2)
    private BigDecimal comissaoGarcon = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "comissao_entregador", precision = 5, scale = 2)
    private BigDecimal comissaoEntregador = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "comissao_cozinheiro", precision = 5, scale = 2)
    private BigDecimal comissaoCozinheiro = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "fechado_manualmente", nullable = false)
    private boolean fechadoManualmente = false;

    @Column(name = "motivo_fechamento_manual", length = 255)
    private String motivoFechamentoManual;

    @Builder.Default
    @Column(name = "alerta_pedido_som", nullable = false, length = 20)
    private String alertaPedidoSom = "SOM_1";

    // Quando true, mensagem nova do WhatsApp toca a notificação falada em vez
    // da padrão (ver NotificationContext no frontend).
    @Builder.Default
    @Column(name = "notificacao_whatsapp_falada", nullable = false)
    private boolean notificacaoWhatsappFalada = false;

    // Frete = freteTaxaBase + freteValorPorKm * distância até o cliente.
    @Builder.Default
    @Column(name = "frete_taxa_base", precision = 10, scale = 2)
    private BigDecimal freteTaxaBase = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "frete_valor_por_km", precision = 10, scale = 2)
    private BigDecimal freteValorPorKm = BigDecimal.ZERO;
}
