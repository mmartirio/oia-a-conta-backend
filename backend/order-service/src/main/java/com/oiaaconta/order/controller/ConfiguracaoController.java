package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.ConfiguracaoRequest;
import com.oiaaconta.order.dto.response.ConfiguracaoResponse;
import com.oiaaconta.order.service.ConfiguracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> get(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(configuracaoService.get(restauranteId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> upsert(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody ConfiguracaoRequest request) {
        return ResponseEntity.ok(configuracaoService.upsert(restauranteId, request));
    }
}
