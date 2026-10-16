package com.oiaaconta.ifood.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Vínculo de um restaurante com uma loja (merchant) do iFood — criado em
// IfoodVinculoService.conectar(). Aplicação Centralizada: accessToken aqui é
// o token de APLICAÇÃO (client_credentials), igual em todas as linhas —
// mantido por merchant só pra não mudar a assinatura de
// garantirTokenValido(merchant) que os outros serviços já usam.
// refreshToken fica sempre null (client_credentials não usa refresh_token).
@Entity
@Table(name = "ifood_merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IfoodMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false, unique = true)
    private Long restauranteId;

    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    @Column(name = "merchant_nome", length = 200)
    private String merchantNome;

    // Cifrado no banco — ver TokenCriptografadoConverter.
    @Column(name = "access_token", columnDefinition = "TEXT")
    @Convert(converter = com.oiaaconta.ifood.util.TokenCriptografadoConverter.class)
    private String accessToken;

    // Cifrado no banco — ver TokenCriptografadoConverter.
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    @Convert(converter = com.oiaaconta.ifood.util.TokenCriptografadoConverter.class)
    private String refreshToken;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Builder.Default
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "conectado_em")
    private LocalDateTime conectadoEm;

    // Último aberto/fechado que o sistema mandou pro iFood — evita reenviar
    // a mesma mudança de status a cada ciclo do IfoodStatusSyncService.
    @Column(name = "ultimo_status_enviado")
    private Boolean ultimoStatusEnviado;

    // Preenchido a cada rodada de IfoodCatalogSyncService — mostrado no
    // admin como "última sincronização".
    @Column(name = "catalogo_sincronizado_em")
    private LocalDateTime catalogoSincronizadoEm;

    // ID da interrupção ativa no iFood quando a loja está fechada por aqui
    // — null quando aberta. Necessário pra saber o que remover na reabertura.
    @Column(name = "interrupcao_ativa_id", length = 100)
    private String interrupcaoAtivaId;
}
