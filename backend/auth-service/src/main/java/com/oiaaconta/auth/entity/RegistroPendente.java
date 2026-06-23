package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_pendentes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_restaurante", nullable = false, length = 200)
    private String nomeRestaurante;

    @Column(name = "nome_admin", nullable = false, length = 100)
    private String nomeAdmin;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(length = 18)
    private String cnpj;

    @Column(length = 20)
    private String telefone;

    @Column(name = "plano_id")
    private Long planoId;

    @Column(nullable = false)
    private LocalDateTime expiradoEm;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
