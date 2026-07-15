package com.oiaaconta.whatsapp.entity;

import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens_whatsapp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensagemWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DirecaoMensagem direcao;

    @Column(length = 2000)
    private String texto;

    @Column(nullable = false)
    @Builder.Default
    private boolean lida = true;

    @CreationTimestamp
    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
}
