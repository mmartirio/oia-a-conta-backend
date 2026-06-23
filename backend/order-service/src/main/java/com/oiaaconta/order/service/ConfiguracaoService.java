package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.ConfiguracaoRequest;
import com.oiaaconta.order.dto.response.ConfiguracaoResponse;
import com.oiaaconta.order.entity.RestauranteConfig;
import com.oiaaconta.order.repository.RestauranteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConfiguracaoService {

    private final RestauranteConfigRepository configRepository;

    public ConfiguracaoResponse get(Long restauranteId) {
        return configRepository.findByRestauranteId(restauranteId)
            .map(this::toResponse)
            .orElse(ConfiguracaoResponse.builder()
                .restauranteId(restauranteId)
                .pixChave(null)
                .comissaoGarcon(BigDecimal.ZERO)
                .comissaoEntregador(BigDecimal.ZERO)
                .comissaoCozinheiro(BigDecimal.ZERO)
                .build());
    }

    @Transactional
    @SuppressWarnings("null")
    public ConfiguracaoResponse upsert(Long restauranteId, ConfiguracaoRequest request) {
        RestauranteConfig config = configRepository.findByRestauranteId(restauranteId)
            .orElse(RestauranteConfig.builder().restauranteId(restauranteId).build());

        if (request.getPixChave() != null) config.setPixChave(request.getPixChave());
        if (request.getComissaoGarcon() != null) config.setComissaoGarcon(request.getComissaoGarcon());
        if (request.getComissaoEntregador() != null) config.setComissaoEntregador(request.getComissaoEntregador());
        if (request.getComissaoCozinheiro() != null) config.setComissaoCozinheiro(request.getComissaoCozinheiro());

        return toResponse(configRepository.save(config));
    }

    private ConfiguracaoResponse toResponse(RestauranteConfig c) {
        return ConfiguracaoResponse.builder()
            .restauranteId(c.getRestauranteId())
            .pixChave(c.getPixChave())
            .comissaoGarcon(c.getComissaoGarcon() != null ? c.getComissaoGarcon() : BigDecimal.ZERO)
            .comissaoEntregador(c.getComissaoEntregador() != null ? c.getComissaoEntregador() : BigDecimal.ZERO)
            .comissaoCozinheiro(c.getComissaoCozinheiro() != null ? c.getComissaoCozinheiro() : BigDecimal.ZERO)
            .build();
    }
}
