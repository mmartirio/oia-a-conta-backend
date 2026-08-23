package com.oiaaconta.order.service;

import com.oiaaconta.order.client.OsrmClient;
import com.oiaaconta.order.dto.osrm.OsrmTripResponse;
import com.oiaaconta.order.dto.response.ParadaRotaResponse;
import com.oiaaconta.order.dto.response.RotaSugeridaResponse;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.enums.StatusEntrega;
import com.oiaaconta.order.repository.EntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

// Sugere ao entregador a ordem de visita das entregas ativas que ele tem no
// momento, considerando distância/tempo (via OSRM) e há quanto tempo cada
// pedido está esperando.
//
// Heurística (não é um solver multi-objetivo de verdade): pedidos esperando
// mais que MINUTOS_LIMITE_URGENTE entram no início da rota, ordenados só
// pelo tempo de espera; o OSRM Trip (TSP aproximado) só otimiza a ordem dos
// pedidos restantes. Isso evita que um pedido antigo fique pra trás só
// porque está geograficamente fora do caminho mais curto.
@Service
@RequiredArgsConstructor
@Slf4j
public class RotaService {

    private static final long MINUTOS_LIMITE_URGENTE = 20;

    // Só vale a pena tirar os pedidos da ordem de chegada se a rota otimizada
    // pelo OSRM economizar pelo menos isso de tempo total — caso contrário,
    // reordenar não compensa o cliente que pediu primeiro esperar mais, e a
    // ordem de chegada (FIFO) prevalece.
    private static final BigDecimal MINUTOS_MINIMOS_GANHO_REORDENACAO = BigDecimal.valueOf(5);

    private final EntregaRepository entregaRepository;
    private final OsrmClient osrmClient;
    private final DistanciaService distanciaService;

    public RotaSugeridaResponse sugerir(Long restauranteId, Long entregadorId, double lat, double lng) {
        List<Entrega> ativas = entregaRepository.findByRestauranteIdAndEntregadorIdAndStatusIn(
            restauranteId, entregadorId,
            List.of(StatusEntrega.ACEITA, StatusEntrega.PRONTO_PARA_ENTREGA, StatusEntrega.SAIU_PARA_ENTREGA));

        List<Entrega> comCoordenadas = new ArrayList<>();
        List<Long> semCoordenadas = new ArrayList<>();
        for (Entrega e : ativas) {
            if (e.getEnderecoLatitude() != null && e.getEnderecoLongitude() != null) {
                comCoordenadas.add(e);
            } else {
                semCoordenadas.add(e.getId());
            }
        }

        if (comCoordenadas.isEmpty()) {
            return RotaSugeridaResponse.builder()
                .paradas(List.of())
                .distanciaTotalKm(BigDecimal.ZERO)
                .duracaoTotalMin(BigDecimal.ZERO)
                .semCoordenadas(semCoordenadas)
                .build();
        }

        LocalDateTime agora = LocalDateTime.now();
        List<Entrega> urgentes = new ArrayList<>();
        List<Entrega> normais = new ArrayList<>();
        for (Entrega e : comCoordenadas) {
            if (Duration.between(e.getCreatedAt(), agora).toMinutes() >= MINUTOS_LIMITE_URGENTE) {
                urgentes.add(e);
            } else {
                normais.add(e);
            }
        }
        urgentes.sort(Comparator.comparing(Entrega::getCreatedAt));

        List<Entrega> normaisOrdenados = normais.size() > 1 ? escolherOrdem(normais, lat, lng) : normais;

        List<Entrega> ordemFinal = new ArrayList<>(urgentes);
        ordemFinal.addAll(normaisOrdenados);

        Set<Long> idsUrgentes = new HashSet<>();
        for (Entrega e : urgentes) idsUrgentes.add(e.getId());

        List<ParadaRotaResponse> paradas = new ArrayList<>();
        BigDecimal distanciaTotal = BigDecimal.ZERO;
        BigDecimal duracaoTotal = BigDecimal.ZERO;
        double origemLat = lat;
        double origemLng = lng;

        for (int i = 0; i < ordemFinal.size(); i++) {
            Entrega e = ordemFinal.get(i);
            DistanciaService.Resultado perna = distanciaService.calcular(
                origemLat, origemLng, e.getEnderecoLatitude(), e.getEnderecoLongitude());

            paradas.add(ParadaRotaResponse.builder()
                .entregaId(e.getId())
                .ordem(i + 1)
                .clienteNome(e.getClienteNome())
                .enderecoResumo(resumoEndereco(e))
                .distanciaKm(perna.distanciaKm())
                .duracaoMin(perna.duracaoMin())
                .urgente(idsUrgentes.contains(e.getId()))
                .build());

            distanciaTotal = distanciaTotal.add(perna.distanciaKm());
            duracaoTotal = duracaoTotal.add(perna.duracaoMin());
            origemLat = e.getEnderecoLatitude();
            origemLng = e.getEnderecoLongitude();
        }

        return RotaSugeridaResponse.builder()
            .paradas(paradas)
            .distanciaTotalKm(distanciaTotal)
            .duracaoTotalMin(duracaoTotal)
            .semCoordenadas(semCoordenadas)
            .build();
    }

    // Resolve a ordem de visita aproximadamente ótima pro grupo de entregas
    // sem urgência via OSRM Trip. Se o OSRM estiver fora do ar ou a resposta
    // vier incompleta, mantém a ordem original (fallback simples, não trava
    // a sugestão de rota).
    //
    // Usa um Map<entregaId, posição> em vez de List.indexOf(Entrega) /
    // List.contains(Entrega) de propósito: Entrega é uma entidade JPA com
    // @Data (equals/hashCode gerados sobre todos os campos, incluindo a
    // coleção "itens" LAZY) — comparar por igualdade de objeto arriscaria
    // inicializar essa coleção fora de contexto.
    private List<Entrega> otimizarComOsrm(List<Entrega> entregas, double origemLat, double origemLng) {
        try {
            StringBuilder coordenadas = new StringBuilder();
            coordenadas.append(String.format(Locale.US, "%f,%f", origemLng, origemLat));
            Map<Long, Integer> posicaoNoRequest = new HashMap<>();
            for (int i = 0; i < entregas.size(); i++) {
                Entrega e = entregas.get(i);
                coordenadas.append(';').append(String.format(Locale.US, "%f,%f", e.getEnderecoLongitude(), e.getEnderecoLatitude()));
                posicaoNoRequest.put(e.getId(), i + 1); // +1: índice 0 é o ponto de origem (entregador)
            }

            OsrmTripResponse resposta = osrmClient.trip(coordenadas.toString(), "first", false);
            if (resposta == null || resposta.getWaypoints() == null
                || resposta.getWaypoints().size() != entregas.size() + 1) {
                return entregas;
            }

            List<OsrmTripResponse.Waypoint> waypoints = resposta.getWaypoints();
            List<Entrega> ordenado = new ArrayList<>(entregas);
            ordenado.sort(Comparator.comparingInt(e -> waypoints.get(posicaoNoRequest.get(e.getId())).getWaypointIndex()));
            return ordenado;
        } catch (Exception e) {
            log.warn("OSRM trip indisponível pra sugestão de rota, mantendo ordem original: {}", e.getMessage());
            return entregas;
        }
    }

    // Compara a ordem otimizada pelo OSRM com a ordem de chegada (FIFO) dos
    // pedidos e só usa a do OSRM se ela render um ganho de tempo perceptível
    // (MINUTOS_MINIMOS_GANHO_REORDENACAO) — caso contrário, prioriza quem
    // pediu primeiro, mais justo e mais previsível pro entregador.
    private List<Entrega> escolherOrdem(List<Entrega> entregas, double origemLat, double origemLng) {
        List<Entrega> porOsrm = otimizarComOsrm(entregas, origemLat, origemLng);
        List<Entrega> porChegada = new ArrayList<>(entregas);
        porChegada.sort(Comparator.comparing(Entrega::getCreatedAt));

        if (mesmaOrdem(porOsrm, porChegada)) return porChegada;

        BigDecimal duracaoOsrm = duracaoTotal(porOsrm, origemLat, origemLng);
        BigDecimal duracaoChegada = duracaoTotal(porChegada, origemLat, origemLng);
        BigDecimal economiaMin = duracaoChegada.subtract(duracaoOsrm);

        return economiaMin.compareTo(MINUTOS_MINIMOS_GANHO_REORDENACAO) > 0 ? porOsrm : porChegada;
    }

    // Compara por ID, não por Entrega.equals() — mesmo cuidado do comentário
    // em otimizarComOsrm (equals() gerado sobre todos os campos, incluindo a
    // coleção "itens" LAZY).
    private boolean mesmaOrdem(List<Entrega> a, List<Entrega> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getId().equals(b.get(i).getId())) return false;
        }
        return true;
    }

    private BigDecimal duracaoTotal(List<Entrega> ordem, double origemLat, double origemLng) {
        BigDecimal total = BigDecimal.ZERO;
        double lat = origemLat;
        double lng = origemLng;
        for (Entrega e : ordem) {
            DistanciaService.Resultado perna = distanciaService.calcular(lat, lng, e.getEnderecoLatitude(), e.getEnderecoLongitude());
            total = total.add(perna.duracaoMin());
            lat = e.getEnderecoLatitude();
            lng = e.getEnderecoLongitude();
        }
        return total;
    }

    private String resumoEndereco(Entrega e) {
        StringBuilder sb = new StringBuilder(e.getEnderecoRua());
        if (e.getEnderecoNumero() != null && !e.getEnderecoNumero().isBlank()) sb.append(", ").append(e.getEnderecoNumero());
        if (e.getEnderecoBairro() != null && !e.getEnderecoBairro().isBlank()) sb.append(" — ").append(e.getEnderecoBairro());
        return sb.toString();
    }
}
