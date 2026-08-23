package com.oiaaconta.order.client;

import com.oiaaconta.order.dto.nominatim.NominatimResultado;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Nominatim (OpenStreetMap) — mesma API pública que o frontend já usa
// client-side (utils/geocoding.ts) pra geocodificar endereço, sem chave. Não
// é um serviço Spring Cloud registrado no Eureka, igual o OsrmClient.
@FeignClient(name = "nominatim", url = "https://nominatim.openstreetmap.org")
public interface NominatimClient {

    @GetMapping("/search")
    List<NominatimResultado> search(
        @RequestParam("q") String query,
        @RequestParam("format") String format,
        @RequestParam("limit") int limit,
        @RequestHeader("User-Agent") String userAgent);
}
