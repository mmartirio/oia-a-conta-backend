package com.oiaaconta.order.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRouteResponse {
    private String code;
    private List<Route> routes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        // Metros e segundos — unidades nativas do OSRM.
        private Double distance;
        private Double duration;
    }
}
