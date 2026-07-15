package com.oiaaconta.order.service;

import com.oiaaconta.order.dto.request.ConfiguracaoRequest;
import com.oiaaconta.order.dto.request.StatusLojaRequest;
import com.oiaaconta.order.dto.request.TaxasMaquininhaRequest;
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
                .fechadoManualmente(false)
                .motivoFechamentoManual(null)
                .alertaPedidoSom("SOM_1")
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
        if (request.getAlertaPedidoSom() != null) config.setAlertaPedidoSom(request.getAlertaPedidoSom());

        return toResponse(configRepository.save(config));
    }

    @Transactional
    @SuppressWarnings("null")
    public ConfiguracaoResponse atualizarStatusLoja(Long restauranteId, StatusLojaRequest request) {
        RestauranteConfig config = configRepository.findByRestauranteId(restauranteId)
            .orElse(RestauranteConfig.builder().restauranteId(restauranteId).build());

        config.setFechadoManualmente(Boolean.TRUE.equals(request.getFechado()));
        config.setMotivoFechamentoManual(config.isFechadoManualmente() ? request.getMotivo() : null);

        return toResponse(configRepository.save(config));
    }

    @Transactional
    @SuppressWarnings("null")
    public ConfiguracaoResponse atualizarTaxasMaquininha(Long restauranteId, TaxasMaquininhaRequest request) {
        RestauranteConfig config = configRepository.findByRestauranteId(restauranteId)
            .orElse(RestauranteConfig.builder().restauranteId(restauranteId).build());

        if (request.getTaxaDebito() != null) config.setTaxaDebito(request.getTaxaDebito());
        if (request.getTaxaCreditoVista() != null) config.setTaxaCreditoVista(request.getTaxaCreditoVista());
        if (request.getTaxaCreditoParcelado() != null) config.setTaxaCreditoParcelado(request.getTaxaCreditoParcelado());

        return toResponse(configRepository.save(config));
    }

    private ConfiguracaoResponse toResponse(RestauranteConfig c) {
        return ConfiguracaoResponse.builder()
            .restauranteId(c.getRestauranteId())
            .pixChave(c.getPixChave())
            .comissaoGarcon(c.getComissaoGarcon() != null ? c.getComissaoGarcon() : BigDecimal.ZERO)
            .comissaoEntregador(c.getComissaoEntregador() != null ? c.getComissaoEntregador() : BigDecimal.ZERO)
            .comissaoCozinheiro(c.getComissaoCozinheiro() != null ? c.getComissaoCozinheiro() : BigDecimal.ZERO)
            .fechadoManualmente(c.isFechadoManualmente())
            .motivoFechamentoManual(c.getMotivoFechamentoManual())
            .alertaPedidoSom(c.getAlertaPedidoSom() != null ? c.getAlertaPedidoSom() : "SOM_1")
            .taxaDebito(c.getTaxaDebito() != null ? c.getTaxaDebito() : BigDecimal.ZERO)
            .taxaCreditoVista(c.getTaxaCreditoVista() != null ? c.getTaxaCreditoVista() : BigDecimal.ZERO)
            .taxaCreditoParcelado(c.getTaxaCreditoParcelado() != null ? c.getTaxaCreditoParcelado() : BigDecimal.ZERO)
            .build();
    }
}
