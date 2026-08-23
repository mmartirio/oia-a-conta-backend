package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.ComboRequest;
import com.oiaaconta.catalog.dto.response.ComboResponse;
import com.oiaaconta.catalog.service.ComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public ResponseEntity<List<ComboResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam(required = false, defaultValue = "true") boolean apenasAtivos) {
        return ResponseEntity.ok(comboService.listar(restauranteId, apenasAtivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComboResponse> buscarPorId(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(comboService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComboResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.status(201).body(comboService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComboResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody ComboRequest request) {
        return ResponseEntity.ok(comboService.atualizar(restauranteId, id, request));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComboResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(comboService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }
}
