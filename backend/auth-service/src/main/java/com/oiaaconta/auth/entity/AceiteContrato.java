package com.oiaaconta.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Registro permanente de auditoria de aceite do Contrato de Adesão/Termos de
// Uso/Política de Privacidade — prova da contratação (versão exata aceita,
// data/hora, IP). Diferente de RegistroPendente (apagado após a verificação
// de e-mail), este registro nunca é removido.
@Entity
@Table(name = "aceites_contrato")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AceiteContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "versao_contrato", nullable = false, length = 20)
    private String versaoContrato;

    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm;

    @Column(name = "ip_aceite", length = 45)
    private String ipAceite;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
