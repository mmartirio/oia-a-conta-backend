package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 20)
    @Builder.Default
    private String plano = "BASICO";

    @Builder.Default
    private boolean ativo = true;

    @Column(name = "email_responsavel", length = 150)
    private String emailResponsavel;

    @Column(length = 18)
    private String cnpj;

    @Column(length = 20)
    private String telefone;

    @Column(name = "whatsapp_instance_name", unique = true, length = 100)
    private String whatsappInstanceName;

    @Builder.Default
    private boolean bloqueado = false;

    @Column(name = "data_bloqueio")
    private LocalDateTime dataBloqueio;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
