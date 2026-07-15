package com.oiaaconta.whatsapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false, unique = true)
    private Long restauranteId;

    @Column(name = "chatbot_ativo", nullable = false)
    @Builder.Default
    private boolean chatbotAtivo = true;
}
