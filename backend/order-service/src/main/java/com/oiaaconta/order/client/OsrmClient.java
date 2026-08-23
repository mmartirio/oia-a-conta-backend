package com.oiaaconta.order.client;

import com.oiaaconta.order.dto.osrm.OsrmRouteResponse;
import com.oiaaconta.order.dto.osrm.OsrmTripResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

// OSRM não é um serviço Spring Cloud registrado no Eureka — a URL é fixa via
// propriedade (osrm.url), não resolvida por nome de serviço como os demais
// clients deste pacote.
@FeignClient(name = "osrm", url = "${osrm.url}")
public interface OsrmClient {

    // Nota: OSRM usa a ordem "longitude,latitude", ao contrário do resto do
    // sistema (que usa latitude,longitude) — inverter isso silenciosamente
    // dá uma rota do outro lado do mundo em vez de erro.
    @GetMapping("/route/v1/driving/{coordenadas}")
    OsrmRouteResponse route(
        @PathVariable("coordenadas") String coordenadas,
        @RequestParam("overview") String overview);

    @GetMapping("/trip/v1/driving/{coordenadas}")
    OsrmTripResponse trip(
        @PathVariable("coordenadas") String coordenadas,
        @RequestParam("source") String source,
        @RequestParam("roundtrip") boolean roundtrip);
}
