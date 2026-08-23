package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.CupomRequest;
import com.oiaaconta.catalog.dto.response.CupomResponse;
import com.oiaaconta.catalog.dto.response.CupomValidacaoResponse;
import com.oiaaconta.catalog.service.CupomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cupons")
@RequiredArgsConstructor
public class CupomController {

    private final CupomService cupomService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CupomResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(cupomService.listar(restauranteId));
    }

    @GetMapping("/validar")
    public ResponseEntity<CupomValidacaoResponse> validar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam String codigo,
            @RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(cupomService.validar(restauranteId, codigo, clienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CupomResponse> buscarPorId(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(cupomService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CupomResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody CupomRequest request) {
        return ResponseEntity.status(201).body(cupomService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CupomResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody CupomRequest request) {
        return ResponseEntity.ok(cupomService.atualizar(restauranteId, id, request));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CupomResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(cupomService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }
}
