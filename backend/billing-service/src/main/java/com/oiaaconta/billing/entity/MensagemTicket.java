package com.oiaaconta.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @JsonIgnore
    private TicketSuporte ticket;

    @Column(name = "remetente_id")
    private Long remetenteId;

    @Column(name = "remetente_nome", length = 100)
    private String remetenteNome;

    @Column(name = "remetente_tipo", length = 20)
    private String remetenteTipo; // CLIENTE | SUPORTE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
