package com.oiaaconta.order.service;

import com.oiaaconta.order.client.AuthClient;
import com.oiaaconta.order.dto.auth.RestauranteLocalizacaoDto;
import com.oiaaconta.order.dto.response.ConfiguracaoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Calcula o frete de uma entrega a partir da distância entre o restaurante e
// o endereço do cliente (via DistanciaService — OSRM com fallback em linha
// reta). Nunca lança — se a localização do restaurante não estiver
// configurada, ou se o endereço do cliente não foi geocodificado no
// frontend, o frete simplesmente fica indisponível (o pedido segue sem
// bloquear).
@Service
@RequiredArgsConstructor
@Slf4j
public class FreteService {

    private final AuthClient authClient;
    private final DistanciaService distanciaService;
    private final ConfiguracaoService configuracaoService;

    public record FreteCalculado(BigDecimal distanciaKm, BigDecimal valorFrete, boolean disponivel) {
        public static FreteCalculado indisponivel() {
            return new FreteCalculado(null, null, false);
        }
    }

    public FreteCalculado calcular(Long restauranteId, Double destinoLat, Double destinoLng) {
        if (destinoLat == null || destinoLng == null) {
            return FreteCalculado.indisponivel();
        }

        RestauranteLocalizacaoDto localizacaoRestaurante;
        try {
            localizacaoRestaurante = authClient.buscarLocalizacao(restauranteId);
        } catch (Exception e) {
            log.warn("Falha ao buscar localização do restaurante #{} pro cálculo de frete: {}", restauranteId, e.getMessage());
            return FreteCalculado.indisponivel();
        }
        if (localizacaoRestaurante == null
            || localizacaoRestaurante.getLatitude() == null
            || localizacaoRestaurante.getLongitude() == null) {
            return FreteCalculado.indisponivel();
        }

        DistanciaService.Resultado distancia = distanciaService.calcular(
            localizacaoRestaurante.getLatitude(), localizacaoRestaurante.getLongitude(), destinoLat, destinoLng);

        ConfiguracaoResponse config = configuracaoService.get(restauranteId);
        BigDecimal taxaBase = config.getFreteTaxaBase() != null ? config.getFreteTaxaBase() : BigDecimal.ZERO;
        BigDecimal valorPorKm = config.getFreteValorPorKm() != null ? config.getFreteValorPorKm() : BigDecimal.ZERO;

        BigDecimal valorFrete = taxaBase.add(valorPorKm.multiply(distancia.distanciaKm())).setScale(2, RoundingMode.HALF_UP);

        return new FreteCalculado(distancia.distanciaKm(), valorFrete, true);
    }
}
