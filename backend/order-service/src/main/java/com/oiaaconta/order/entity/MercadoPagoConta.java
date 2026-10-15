package com.oiaaconta.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Conta Mercado Pago do restaurante, conectada via OAuth — recebe o
// pagamento online dos pedidos do cardápio. Tokens sempre cifrados (ver
// TokenCifrador); nunca serializar esta entidade pra fora do serviço.
@Entity
@Table(name = "mercadopago_contas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MercadoPagoConta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false, unique = true)
    private Long restauranteId;

    @Column(name = "mp_user_id", length = 50)
    private String mpUserId;

    @Column(name = "access_token_cifrado", columnDefinition = "TEXT")
    private String accessTokenCifrado;

    @Column(name = "refresh_token_cifrado", columnDefinition = "TEXT")
    private String refreshTokenCifrado;

    @Column(name = "public_key", length = 100)
    private String publicKey;

    @Column(name = "expira_em")
    private LocalDateTime expiraEm;

    @Column(name = "conectado_em")
    private LocalDateTime conectadoEm;

    @Column(name = "oauth_state", length = 100)
    private String oauthState;

    @Column(name = "oauth_state_expira_em")
    private LocalDateTime oauthStateExpiraEm;

    public boolean isConectada() {
        return accessTokenCifrado != null && publicKey != null;
    }
}
