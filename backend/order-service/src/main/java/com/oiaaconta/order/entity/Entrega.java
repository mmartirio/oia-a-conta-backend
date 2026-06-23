package com.oiaaconta.order.entity;

import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.enums.StatusEntrega;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entregas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "cliente_nome", nullable = false, length = 100)
    private String clienteNome;

    @Column(name = "cliente_telefone", length = 20)
    private String clienteTelefone;

    @Column(name = "endereco_rua", nullable = false, length = 200)
    private String enderecoRua;

    @Column(name = "endereco_numero", nullable = false, length = 20)
    private String enderecoNumero;

    @Column(name = "endereco_bairro", length = 100)
    private String enderecoBairro;

    @Column(name = "endereco_cidade", nullable = false, length = 100)
    private String enderecoCidade;

    @Column(name = "endereco_complemento", length = 100)
    private String enderecoComplemento;

    @Column(name = "entregador_id")
    private Long entregadorId;

    @Column(name = "entregador_nome", length = 100)
    private String entregadorNome;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusEntrega status = StatusEntrega.AGUARDANDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false)
    private MetodoPagamento metodoPagamento;

    private Integer parcelas;

    @Column(length = 500)
    private String observacao;

    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ItemEntrega> itens = new ArrayList<>();

    @Column(name = "pedido_cozinha_id")
    private Long pedidoCozinhaId;

    @Builder.Default
    @Column(name = "origem_whatsapp", nullable = false)
    private Boolean origemWhatsapp = false;

    @Builder.Default
    @Column(name = "pagamento_confirmado_caixa", nullable = false)
    private Boolean pagamentoConfirmadoCaixa = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "entregue_at")
    private LocalDateTime entregueAt;
}
