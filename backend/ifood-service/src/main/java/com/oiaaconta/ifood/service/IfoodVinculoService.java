package com.oiaaconta.ifood.service;

import com.oiaaconta.ifood.client.IfoodAuthClient;
import com.oiaaconta.ifood.client.IfoodMerchantClient;
import com.oiaaconta.ifood.config.IfoodProperties;
import com.oiaaconta.ifood.dto.ifood.IfoodMerchantDto;
import com.oiaaconta.ifood.dto.ifood.IfoodTokenResponse;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.exception.BusinessException;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Vínculo de loja do iFood — aplicação cadastrada como CENTRALIZADA no
// portal deles (SaaS, uma instância só atendendo várias lojas), não
// distribuída. Isso muda o mecanismo de autenticação/autorização:
//
// - Autenticação da aplicação: grant client_credentials (clientId/clientSecret
//   direto, sem userCode/authorization_code) — token de aplicação, não de
//   loja específica, mas continua sendo salvo em cada IfoodMerchant (campo
//   accessToken) pra não precisar mexer nos outros serviços que já esperam
//   "um token válido por merchant" (IfoodCatalogSyncService,
//   IfoodStatusSyncService, IfoodEventsPoller etc.) — o conteúdo é o mesmo
//   token de app em todas as linhas, só a validade que é controlada por
//   linha.
// - Autorização por loja: pra apps centralizados, o dono da loja autoriza
//   a aplicação pelo Portal do Parceiro do iFood (marketplace) — não existe
//   mais o passo de gerar/confirmar userCode aqui. O que o backend faz é
//   listar (GET /merchant/v1.0/merchants) as lojas já autorizadas pra essa
//   aplicação e deixar o admin escolher qual é a dele — ver
//   listarLojasDisponiveis()/conectar().
@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodVinculoService {

    private final IfoodAuthClient authClient;
    private final IfoodMerchantClient merchantClient;
    private final IfoodMerchantRepository merchantRepository;
    private final IfoodProperties properties;

    // Lojas autorizadas pra essa aplicação no iFood, mas que ainda não foram
    // reivindicadas por nenhum restaurante daqui — é dessa lista que o admin
    // escolhe qual é a loja dele em conectar().
    public List<IfoodMerchantDto> listarLojasDisponiveis() {
        exigirCredenciaisConfiguradas();
        String token = obterTokenAplicacao();
        List<IfoodMerchantDto> todas = merchantClient.listarMerchants("Bearer " + token);

        Set<String> jaVinculadas = merchantRepository.findByAtivoTrue().stream()
            .map(IfoodMerchant::getMerchantId)
            .collect(Collectors.toSet());

        return todas.stream()
            .filter(m -> !jaVinculadas.contains(m.getId()))
            .toList();
    }

    @Transactional
    @SuppressWarnings("null")
    public void conectar(Long restauranteId, String merchantId) {
        exigirCredenciaisConfiguradas();
        if (merchantRepository.findByRestauranteId(restauranteId).filter(IfoodMerchant::isAtivo).isPresent()) {
            throw new BusinessException("Este restaurante já está conectado a uma loja do iFood");
        }

        String token = obterTokenAplicacao();
        IfoodMerchantDto merchant = merchantClient.listarMerchants("Bearer " + token).stream()
            .filter(m -> m.getId().equals(merchantId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(
                "Essa loja não está autorizada pra esta aplicação no iFood. Autorize primeiro pelo Portal do Parceiro."));

        if (merchantRepository.findByAtivoTrue().stream()
                .anyMatch(m -> merchantId.equals(m.getMerchantId()))) {
            throw new BusinessException("Essa loja do iFood já está conectada a outro restaurante");
        }

        IfoodMerchant entidade = merchantRepository.findByRestauranteId(restauranteId)
            .orElse(IfoodMerchant.builder().restauranteId(restauranteId).build());
        entidade.setMerchantId(merchant.getId());
        entidade.setMerchantNome(merchant.getName());
        entidade.setAccessToken(token);
        entidade.setRefreshToken(null);
        entidade.setExpiraEm(LocalDateTime.now().plusMinutes(50));
        entidade.setAtivo(true);
        entidade.setConectadoEm(LocalDateTime.now());
        merchantRepository.save(entidade);
    }

    public void desconectar(Long restauranteId) {
        merchantRepository.findByRestauranteId(restauranteId).ifPresent(m -> {
            m.setAtivo(false);
            merchantRepository.save(m);
        });
    }

    // Garante um token de aplicação válido — chamado por merchant (linha do
    // IfoodMerchant) pra não mexer na assinatura que os outros serviços já
    // usam, mas o token em si é o mesmo pra qualquer loja (client_credentials
    // não é por merchant). Renovado sempre via client_credentials — grant
    // refresh_token não existe pra esse tipo de aplicação.
    public String garantirTokenValido(IfoodMerchant merchant) {
        if (merchant.getExpiraEm() != null && merchant.getExpiraEm().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return merchant.getAccessToken();
        }
        String token = obterTokenAplicacao();
        merchant.setAccessToken(token);
        merchant.setExpiraEm(LocalDateTime.now().plusMinutes(50));
        merchantRepository.save(merchant);
        return token;
    }

    private String obterTokenAplicacao() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grantType", "client_credentials");
        form.put("clientId", properties.getClientId());
        form.put("clientSecret", properties.getClientSecret());
        IfoodTokenResponse token = authClient.obterToken(formBody(form));
        return token.getAccessToken();
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
