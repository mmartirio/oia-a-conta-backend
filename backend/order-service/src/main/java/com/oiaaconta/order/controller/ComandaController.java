package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.ConfirmarPagamentoRequest;
import com.oiaaconta.order.dto.response.ComandaResponse;
import com.oiaaconta.order.service.ComandaService;
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
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN','CAIXA')")
    public ResponseEntity<List<ComandaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(comandaService.listarAbertas(restauranteId));
    }

    @GetMapping("/api/comandas/aguardando-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ComandaResponse>> aguardandoPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(comandaService.listarAguardandoPagamento(restauranteId));
    }

    @PutMapping("/api/comandas/{id}/fechar")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> fechar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        return ResponseEntity.ok(comandaService.fecharComanda(restauranteId, id, authHeader));
    }

    @PutMapping("/api/comandas/{id}/confirmar-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ComandaResponse> confirmarPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarPagamentoRequest request) {
        return ResponseEntity.ok(comandaService.confirmarPagamento(restauranteId, id, request, authHeader));
    }
}
