package com.comandadigital.order.entity;

import com.comandadigital.order.enums.MetodoPagamento;
import com.comandadigital.order.enums.StatusComanda;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comandas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "mesa_id", nullable = false)
    private Long mesaId;

    @Column(name = "mesa_numero", nullable = false)
    private Integer mesaNumero;

    @Column(name = "garcon_id", nullable = false)
    private Long garconId;

    @Column(name = "garcon_nome", nullable = false, length = 100)
    private String garconNome;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusComanda status = StatusComanda.ABERTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento")
    private MetodoPagamento metodoPagamento;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
