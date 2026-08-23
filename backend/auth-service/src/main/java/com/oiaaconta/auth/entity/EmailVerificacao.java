package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verificacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime expiradoEm;

    @Builder.Default
    private boolean usado = false;

    // Conta tentativas de código errado — limita brute-force do código de 6
    // dígitos (ver AuthService.verificarEmail, invalida após MAX_TENTATIVAS).
    @Builder.Default
    private int tentativas = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isValido() {
        return !usado && LocalDateTime.now().isBefore(expiradoEm);
    }
}
