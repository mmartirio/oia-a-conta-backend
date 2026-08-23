package com.oiaaconta.catalog.entity;

import com.oiaaconta.catalog.enums.TipoAlvo;
import com.oiaaconta.catalog.enums.TipoDesconto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desconto", nullable = false, length = 10)
    private TipoDesconto tipoDesconto;

    @Column(name = "valor_desconto", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDesconto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alvo", nullable = false, length = 12)
    private TipoAlvo tipoAlvo;

    @Column(name = "grupo_cliente_id")
    private Long grupoClienteId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "valido_de", nullable = false)
    private LocalDate validoDe;

    @Column(name = "valido_ate", nullable = false)
    private LocalDate validoAte;

    @Builder.Default
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
