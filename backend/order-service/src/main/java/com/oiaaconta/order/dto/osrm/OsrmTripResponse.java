package com.oiaaconta.order.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

// Resposta do serviço /trip do OSRM — resolve um TSP aproximado dado um
// conjunto de coordenadas. `waypoints[i].waypointIndex` é a posição de
// visita otimizada (0-based) da coordenada que foi enviada na posição i do
// request — é essa indireção que usamos pra remontar a ordem sugerida.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmTripResponse {
    private String code;
    private List<Trip> trips;
    private List<Waypoint> waypoints;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Trip {
        private Double distance;
        private Double duration;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Waypoint {
        @JsonProperty("waypoint_index")
        private Integer waypointIndex;
    }
}
