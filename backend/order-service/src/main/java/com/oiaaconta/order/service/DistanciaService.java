package com.oiaaconta.order.service;

import com.oiaaconta.order.client.OsrmClient;
import com.oiaaconta.order.dto.osrm.OsrmRouteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

// Distância/tempo entre dois pontos via OSRM (rota real por ruas), usado
// tanto pelo cálculo de frete (FreteService) quanto pela sugestão de rota
// (RotaService). Se o OSRM estiver fora do ar, cai pra uma estimativa em
// linha reta (Haversine) com uma velocidade média assumida — nunca lança.
@Service
@RequiredArgsConstructor
@Slf4j
public class DistanciaService {

    private static final double RAIO_TERRA_KM = 6371.0;
    private static final double VELOCIDADE_MEDIA_FALLBACK_KMH = 25.0;

    private final OsrmClient osrmClient;

    public record Resultado(BigDecimal distanciaKm, BigDecimal duracaoMin) {}

    public Resultado calcular(double origemLat, double origemLng, double destinoLat, double destinoLng) {
        try {
            // OSRM espera "longitude,latitude" (ordem GeoJSON), invertida em
            // relação ao resto do sistema.
            String coordenadas = String.format(Locale.US, "%f,%f;%f,%f", origemLng, origemLat, destinoLng, destinoLat);
            OsrmRouteResponse resposta = osrmClient.route(coordenadas, "false");
            if (resposta != null && resposta.getRoutes() != null && !resposta.getRoutes().isEmpty()) {
                OsrmRouteResponse.Route rota = resposta.getRoutes().get(0);
                if (rota.getDistance() != null && rota.getDuration() != null) {
                    return new Resultado(arredondar(rota.getDistance() / 1000.0), arredondar(rota.getDuration() / 60.0));
                }
            }
        } catch (Exception e) {
            log.warn("OSRM indisponível, caindo pra estimativa em linha reta: {}", e.getMessage());
        }

        double distanciaKm = haversineKm(origemLat, origemLng, destinoLat, destinoLng);
        double duracaoMin = distanciaKm / VELOCIDADE_MEDIA_FALLBACK_KMH * 60;
        return new Resultado(arredondar(distanciaKm), arredondar(duracaoMin));
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_KM * c;
    }

    private BigDecimal arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
