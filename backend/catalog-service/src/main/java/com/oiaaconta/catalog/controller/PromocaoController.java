package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.PromocaoRequest;
import com.oiaaconta.catalog.dto.response.PromocaoAplicavelResponse;
import com.oiaaconta.catalog.dto.response.PromocaoResponse;
import com.oiaaconta.catalog.service.PromocaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/promocoes")
@RequiredArgsConstructor
public class PromocaoController {

    private final PromocaoService promocaoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PromocaoResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(promocaoService.listar(restauranteId));
    }

    @GetMapping("/aplicaveis")
    public ResponseEntity<List<PromocaoAplicavelResponse>> aplicaveis(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) BigDecimal gastoHistorico) {
        return ResponseEntity.ok(promocaoService.aplicaveis(restauranteId, clienteId, gastoHistorico));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PromocaoResponse> buscarPorId(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(promocaoService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PromocaoResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody PromocaoRequest request) {
        return ResponseEntity.status(201).body(promocaoService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PromocaoResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody PromocaoRequest request) {
        return ResponseEntity.ok(promocaoService.atualizar(restauranteId, id, request));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PromocaoResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(promocaoService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }
}
