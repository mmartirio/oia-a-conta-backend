package com.oiaaconta.order.service;

import com.oiaaconta.order.client.MercadoPagoApi;
import com.oiaaconta.order.entity.MercadoPagoConta;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.repository.MercadoPagoContaRepository;
import com.oiaaconta.order.util.TokenCifrador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

// Conexão da conta Mercado Pago do restaurante via OAuth (o dono autoriza
// a aplicação do Oia a Conta a criar pagamentos em nome dele). O dinheiro
// cai direto na conta do restaurante — sem marketplace_fee.
@Service
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoContaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MercadoPagoContaRepository contaRepository;
    private final MercadoPagoApi mercadoPagoApi;
    private final TokenCifrador cifrador;

    @Value("${mercadopago.client-id:}")
    private String clientId;

    @Value("${mercadopago.client-secret:}")
    private String clientSecret;

    // URL do frontend que recebe o ?code=&state= (AdminMercadoPagoCallback)
    // — precisa ser IDÊNTICA à cadastrada em "Redirect URL" na aplicação do MP.
    @Value("${mercadopago.oauth-redirect-uri:}")
    private String redirectUri;

    public Optional<MercadoPagoConta> contaConectada(Long restauranteId) {
        return contaRepository.findByRestauranteId(restauranteId).filter(MercadoPagoConta::isConectada);
    }

    public Map<String, Object> status(Long restauranteId) {
        Map<String, Object> r = new HashMap<>();
        Optional<MercadoPagoConta> conta = contaConectada(restauranteId);
        r.put("disponivel", oauthConfigurado());
        r.put("conectado", conta.isPresent());
        conta.ifPresent(c -> {
            r.put("mpUserId", c.getMpUserId());
            r.put("conectadoEm", c.getConectadoEm());
        });
        return r;
    }

    @Transactional
    public String iniciarConexao(Long restauranteId) {
        if (!oauthConfigurado()) {
            throw new BusinessException("Pagamento online ainda não está disponível. Fale com o suporte.");
        }
        MercadoPagoConta conta = contaRepository.findByRestauranteId(restauranteId)
            .orElseGet(() -> MercadoPagoConta.builder().restauranteId(restauranteId).build());
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String state = HexFormat.of().formatHex(bytes);
        conta.setOauthState(state);
        conta.setOauthStateExpiraEm(LocalDateTime.now().plusMinutes(15));
        contaRepository.save(conta);

        return UriComponentsBuilder.fromHttpUrl("https://auth.mercadopago.com.br/authorization")
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("platform_id", "mp")
            .queryParam("state", state)
            .queryParam("redirect_uri", redirectUri)
            .encode().build().toUriString();
    }

    // O state amarra o retorno do MP ao restaurante de quem iniciou — sem
    // isso, um atacante poderia fazer o dono de outro restaurante vincular
    // a conta MP DO ATACANTE (e receber os pagamentos dele).
    @Transactional
    public void concluirConexao(Long restauranteId, String code, String state) {
        MercadoPagoConta conta = contaRepository.findByRestauranteId(restauranteId)
            .orElseThrow(() -> new BusinessException("Nenhuma conexão em andamento. Tente conectar de novo."));
        if (state == null || conta.getOauthState() == null || !conta.getOauthState().equals(state)
                || conta.getOauthStateExpiraEm() == null || conta.getOauthStateExpiraEm().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Autorização expirada ou inválida. Tente conectar de novo.");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException("O Mercado Pago não devolveu a autorização.");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", redirectUri);
        Map<String, Object> token = mercadoPagoApi.post(null, "/oauth/token", body);

        aplicarToken(conta, token);
        conta.setConectadoEm(LocalDateTime.now());
        conta.setOauthState(null);
        conta.setOauthStateExpiraEm(null);
        contaRepository.save(conta);
        log.info("Restaurante {} conectou a conta Mercado Pago {}", restauranteId, conta.getMpUserId());
    }

    @Transactional
    public void desconectar(Long restauranteId) {
        contaRepository.findByRestauranteId(restauranteId).ifPresent(conta -> {
            conta.setAccessTokenCifrado(null);
            conta.setRefreshTokenCifrado(null);
            conta.setPublicKey(null);
            conta.setExpiraEm(null);
            conta.setMpUserId(null);
            conta.setConectadoEm(null);
            contaRepository.save(conta);
            log.info("Restaurante {} desconectou a conta Mercado Pago", restauranteId);
        });
    }

    public String accessToken(MercadoPagoConta conta) {
        return cifrador.decifrar(conta.getAccessTokenCifrado());
    }

    // Token do OAuth vale 180 dias — renova os que vencem nos próximos 30,
    // uma vez por dia. Falha de renovação só loga (tenta de novo amanhã); se
    // vencer de vez, o pagamento online some do cardápio até reconectar.
    @Scheduled(cron = "0 30 4 * * *", zone = "America/Sao_Paulo")
    public void renovarTokens() {
        if (!oauthConfigurado()) return;
        for (MercadoPagoConta conta : contaRepository.findByRefreshTokenCifradoNotNullAndExpiraEmBefore(LocalDateTime.now().plusDays(30))) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("client_id", clientId);
                body.put("client_secret", clientSecret);
                body.put("grant_type", "refresh_token");
                body.put("refresh_token", cifrador.decifrar(conta.getRefreshTokenCifrado()));
                aplicarToken(conta, mercadoPagoApi.post(null, "/oauth/token", body));
                contaRepository.save(conta);
                log.info("Token Mercado Pago do restaurante {} renovado", conta.getRestauranteId());
            } catch (Exception e) {
                log.warn("Falha ao renovar token Mercado Pago do restaurante {}: {}", conta.getRestauranteId(), e.getMessage());
            }
        }
    }

    private void aplicarToken(MercadoPagoConta conta, Map<String, Object> token) {
        Object accessToken = token.get("access_token");
        Object publicKey = token.get("public_key");
        if (accessToken == null || publicKey == null) {
            throw new BusinessException("Resposta inesperada do Mercado Pago ao conectar a conta.");
        }
        conta.setAccessTokenCifrado(cifrador.cifrar(accessToken.toString()));
        Object refresh = token.get("refresh_token");
        if (refresh != null) conta.setRefreshTokenCifrado(cifrador.cifrar(refresh.toString()));
        conta.setPublicKey(publicKey.toString());
        if (token.get("user_id") != null) conta.setMpUserId(token.get("user_id").toString());
        long expiresIn = token.get("expires_in") instanceof Number n ? n.longValue() : 15552000L;
        conta.setExpiraEm(LocalDateTime.now().plusSeconds(expiresIn));
    }

    private boolean oauthConfigurado() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank()
            && redirectUri != null && !redirectUri.isBlank() && cifrador.configurado();
    }
}
