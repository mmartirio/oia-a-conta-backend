package com.comandadigital.order.controller;

import com.comandadigital.order.dto.request.FecharComandaRequest;
import com.comandadigital.order.dto.response.ComandaResponse;
import com.comandadigital.order.security.JwtUtil;
import com.comandadigital.order.service.ComandaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ComandaController {

    private final ComandaService comandaService;
    private final JwtUtil jwtUtil;

    @PostMapping("/api/mesas/{mesaId}/comanda")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> abrir(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long mesaId,
            @RequestParam Integer mesaNumero,
            @RequestParam Long garconId,
            @RequestParam String garconNome) {
        return ResponseEntity.status(201).body(
            comandaService.abrirComanda(restauranteId, mesaId, mesaNumero, garconId, garconNome, authHeader));
    }

    @GetMapping("/api/mesas/{mesaId}/comanda")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','COZINHA','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> buscarAtiva(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long mesaId) {
        return ResponseEntity.ok(comandaService.buscarComandaAtiva(restauranteId, mesaId));
    }

    @GetMapping("/api/comandas/{id}")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','COZINHA','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> buscar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(comandaService.buscarPorId(restauranteId, id));
    }

    @GetMapping("/api/comandas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ComandaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(comandaService.listarAbertas(restauranteId));
    }

    @PutMapping("/api/comandas/{id}/fechar")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> fechar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody FecharComandaRequest request) {
        return ResponseEntity.ok(comandaService.fecharComanda(restauranteId, id, request, authHeader));
    }
}
