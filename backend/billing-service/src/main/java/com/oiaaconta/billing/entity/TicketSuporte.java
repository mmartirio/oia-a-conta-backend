package com.oiaaconta.billing.entity;

import com.oiaaconta.billing.enums.PrioridadeTicket;
import com.oiaaconta.billing.enums.StatusTicket;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets_suporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSuporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nulo para tickets originados por WhatsApp (sem restaurante vinculado —
    // ver whatsappTelefone).
    @Column(name = "restaurante_id")
    private Long restauranteId;

    @Column(name = "restaurante_nome", length = 200)
    private String restauranteNome;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "whatsapp_telefone", length = 30)
    private String whatsappTelefone;

    @Column(name = "whatsapp_nome", length = 150)
    private String whatsappNome;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String origem = "PAINEL"; // PAINEL | WHATSAPP

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusTicket status = StatusTicket.ABERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PrioridadeTicket prioridade = PrioridadeTicket.MEDIA;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MensagemTicket> mensagens = new ArrayList<>();

    @Column(name = "resolvido_at")
    private LocalDateTime resolvidoAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
