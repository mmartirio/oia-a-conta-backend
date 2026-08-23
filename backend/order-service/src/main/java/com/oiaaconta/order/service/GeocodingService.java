package com.oiaaconta.order.service;

import com.oiaaconta.order.client.NominatimClient;
import com.oiaaconta.order.dto.nominatim.NominatimResultado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// Geocodifica o endereço de entrega quando ele chega sem coordenadas — hoje
// só acontece com pedidos originados do WhatsApp/cardápio público, cujo
// checkout não geocodifica no cliente (diferente do PDV, que já manda
// lat/lng prontos). Sem coordenadas, tanto o frete por distância
// (FreteService) quanto a sugestão de rota (RotaService) ficam indisponíveis
// pro pedido — ver EntregaService.criar.
//
// Usa a mesma API pública (Nominatim/OpenStreetMap) que o frontend já usa
// client-side, com User-Agent identificando a aplicação conforme a política
// de uso deles. Best-effort: falha silenciosa (log + null), nunca impede a
// criação do pedido.
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private static final String USER_AGENT = "OiaAConta/1.0 (delivery geocoding; contato via app)";

    private final NominatimClient nominatimClient;

    public record Coordenada(double lat, double lng) {}

    public Coordenada geocodificar(String endereco) {
        if (endereco == null || endereco.isBlank()) return null;
        try {
            List<NominatimResultado> resultados = nominatimClient.search(endereco, "json", 1, USER_AGENT);
            if (resultados == null || resultados.isEmpty()) return null;
            NominatimResultado r = resultados.get(0);
            return new Coordenada(Double.parseDouble(r.getLat()), Double.parseDouble(r.getLon()));
        } catch (Exception e) {
            log.warn("Falha ao geocodificar endereço '{}': {}", endereco, e.getMessage());
            return null;
        }
    }
}
