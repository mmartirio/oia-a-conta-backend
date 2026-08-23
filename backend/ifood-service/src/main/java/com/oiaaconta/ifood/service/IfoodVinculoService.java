package com.oiaaconta.ifood.service;

import com.oiaaconta.ifood.client.IfoodAuthClient;
import com.oiaaconta.ifood.client.IfoodMerchantClient;
import com.oiaaconta.ifood.config.IfoodProperties;
import com.oiaaconta.ifood.dto.ifood.IfoodMerchantDto;
import com.oiaaconta.ifood.dto.ifood.IfoodTokenResponse;
import com.oiaaconta.ifood.dto.ifood.IfoodUserCodeResponse;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.enums.IfoodVinculoStatus;
import com.oiaaconta.ifood.exception.BusinessException;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Fluxo de vínculo de loja do iFood pra integradores de PDV/ERP: a
// aplicação (Client ID/Secret cadastrados uma vez no portal deles) gera um
// código que o admin do restaurante confirma no painel do iFood; depois
// disso a aplicação troca esse código por um token de acesso já autorizado
// pra(s) loja(s) daquela conta.
//
// Aviso: os nomes exatos dos parâmetros/endpoints seguem a Authentication
// API do iFood pelo meu conhecimento geral dela, não uma consulta à doc
// atual — conferir contra a doc/sandbox reais antes de ir pra produção.
@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodVinculoService {

    private final IfoodAuthClient authClient;
    private final IfoodMerchantClient merchantClient;
    private final IfoodMerchantRepository merchantRepository;
    private final IfoodProperties properties;

    // Estado do vínculo em andamento — vive só na memória enquanto o admin
    // não confirma no painel do iFood (poucos minutos); não precisa de
    // tabela própria, e se o processo reiniciar o admin só clica de novo.
    private final Map<Long, PendenteVinculo> pendentes = new ConcurrentHashMap<>();

    private record PendenteVinculo(String userCode, String verifier, Instant expiraEm) {}

    public IfoodUserCodeResponse iniciar(Long restauranteId) {
        exigirCredenciaisConfiguradas();

        String form = formBody(Map.of("clientId", properties.getClientId()));
        IfoodUserCodeResponse resp = authClient.gerarUserCode(form);

        pendentes.put(restauranteId, new PendenteVinculo(
            resp.getUserCode(), resp.getAuthorizationCodeVerifier(),
            Instant.now().plusSeconds(resp.getExpiresIn() != null ? resp.getExpiresIn() : 600)));

        return resp;
    }

    // Chamado em poll pelo frontend enquanto o admin não confirma no painel
    // do iFood — tenta trocar o código pelo token; se ainda não foi
    // autorizado, o iFood recusa e o status continua "aguardando".
    public IfoodVinculoStatus verificar(Long restauranteId) {
        PendenteVinculo pendente = pendentes.get(restauranteId);
        if (pendente == null) {
            return merchantRepository.findByRestauranteId(restauranteId).map(m -> m.isAtivo()
                ? IfoodVinculoStatus.CONECTADO
                : IfoodVinculoStatus.NAO_INICIADO).orElse(IfoodVinculoStatus.NAO_INICIADO);
        }
        if (Instant.now().isAfter(pendente.expiraEm())) {
            pendentes.remove(restauranteId);
            return IfoodVinculoStatus.EXPIRADO;
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grantType", "authorization_code");
        form.put("clientId", properties.getClientId());
        form.put("clientSecret", properties.getClientSecret());
        form.put("authorizationCode", pendente.userCode());
        form.put("authorizationCodeVerifier", pendente.verifier());

        IfoodTokenResponse token;
        try {
            token = authClient.obterToken(formBody(form));
        } catch (Exception e) {
            // Ainda não autorizado no painel do iFood — normal durante o poll.
            log.debug("Vínculo iFood ainda pendente pro restaurante {}: {}", restauranteId, e.getMessage());
            return IfoodVinculoStatus.AGUARDANDO_AUTORIZACAO;
        }

        List<IfoodMerchantDto> merchants = merchantClient.listarMerchants("Bearer " + token.getAccessToken());
        if (merchants.isEmpty()) {
            throw new BusinessException("Nenhuma loja liberada pra essa conta do iFood");
        }
        IfoodMerchantDto merchant = merchants.get(0);

        IfoodMerchant entidade = merchantRepository.findByRestauranteId(restauranteId)
            .orElse(IfoodMerchant.builder().restauranteId(restauranteId).build());
        entidade.setMerchantId(merchant.getId());
        entidade.setMerchantNome(merchant.getName());
        entidade.setAccessToken(token.getAccessToken());
        entidade.setRefreshToken(token.getRefreshToken());
        entidade.setExpiraEm(LocalDateTime.now().plusSeconds(token.getExpiresIn() != null ? token.getExpiresIn() : 21600));
        entidade.setAtivo(true);
        entidade.setConectadoEm(LocalDateTime.now());
        merchantRepository.save(entidade);

        pendentes.remove(restauranteId);
        return IfoodVinculoStatus.CONECTADO;
    }

    public void desconectar(Long restauranteId) {
        merchantRepository.findByRestauranteId(restauranteId).ifPresent(m -> {
            m.setAtivo(false);
            merchantRepository.save(m);
        });
        pendentes.remove(restauranteId);
    }

    // Garante um access token válido pro merchant, renovando via
    // refresh_token se estiver perto de expirar. Usado por qualquer chamada
    // autenticada às demais APIs do iFood (catálogo, pedidos, status).
    public String garantirTokenValido(IfoodMerchant merchant) {
        if (merchant.getExpiraEm() != null && merchant.getExpiraEm().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return merchant.getAccessToken();
        }

        Map<String, String> form = new LinkedHashMap<>();
        form.put("grantType", "refresh_token");
        form.put("clientId", properties.getClientId());
        form.put("clientSecret", properties.getClientSecret());
        form.put("refreshToken", merchant.getRefreshToken());

        IfoodTokenResponse token = authClient.obterToken(formBody(form));
        merchant.setAccessToken(token.getAccessToken());
        merchant.setRefreshToken(token.getRefreshToken());
        merchant.setExpiraEm(LocalDateTime.now().plusSeconds(token.getExpiresIn() != null ? token.getExpiresIn() : 21600));
        merchantRepository.save(merchant);
        return merchant.getAccessToken();
    }

    private void exigirCredenciaisConfiguradas() {
        if (!properties.configurada()) {
            throw new BusinessException("IFOOD_CLIENT_ID/IFOOD_CLIENT_SECRET não configurados nesta instância");
        }
    }

    // Monta o corpo x-www-form-urlencoded na mão (key1=val1&key2=val2, cada
    // valor com URLEncoder) — o encoder padrão do Feign pra MultiValueMap não
    // estava de fato serializando os campos (ver comentário em
    // IfoodAuthClient), então construir a string exata elimina a ambiguidade.
    private static String formBody(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=')
                .append(URLEncoder.encode(e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
