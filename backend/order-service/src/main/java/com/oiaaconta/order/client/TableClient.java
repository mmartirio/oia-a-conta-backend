package com.oiaaconta.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "table-service", path = "/api/mesas")
public interface TableClient {

    @PutMapping("/{id}/status")
    ResponseEntity<Object> atualizarStatus(
        @RequestHeader("Authorization") String authorization,
        @RequestHeader("X-Restaurante-Id") Long restauranteId,
        @PathVariable Long id,
        @RequestBody Map<String, String> body);
}
