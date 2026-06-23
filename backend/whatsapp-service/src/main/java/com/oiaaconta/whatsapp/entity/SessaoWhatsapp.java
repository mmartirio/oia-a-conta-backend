package com.oiaaconta.whatsapp.entity;

import com.oiaaconta.whatsapp.enums.EstadoSessao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessoes_whatsapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessaoWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSessao estado = EstadoSessao.INICIO;

    @Column(name = "cliente_nome", length = 100)
    private String clienteNome;

    @Column(name = "endereco_rua", length = 200)
    private String enderecoRua;

    @Column(name = "endereco_numero", length = 20)
    private String enderecoNumero;

    @Column(name = "endereco_bairro", length = 100)
    private String enderecoBairro;

    @Column(name = "endereco_cidade", length = 100)
    private String enderecoCidade;

    @Column(name = "endereco_complemento", length = 100)
    private String enderecoComplemento;

    @Column(name = "metodo_pagamento", length = 30)
    private String metodoPagamento;

    @Column(name = "categoria_selecionada_id")
    private Long categoriaSelecionadaId;

    @Column(name = "produto_selecionado_id")
    private Long produtoSelecionadoId;

    @Column(length = 500)
    private String observacao;

    @Column(name = "entrega_id")
    private Long entregaId;

    @Column(name = "erros_consecutivos")
    @Builder.Default
    private Integer errosConsecutivos = 0;

    @UpdateTimestamp
    @Column(name = "ultima_interacao")
    private LocalDateTime ultimaInteracao;

    @OneToMany(mappedBy = "sessao", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<ItemCarrinho> itens = new ArrayList<>();
}
