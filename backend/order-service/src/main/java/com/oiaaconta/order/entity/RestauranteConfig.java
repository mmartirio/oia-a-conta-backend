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
}
