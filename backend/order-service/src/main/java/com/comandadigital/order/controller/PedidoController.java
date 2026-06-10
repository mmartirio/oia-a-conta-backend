package com.comandadigital.order.controller;

import com.comandadigital.order.dto.request.PedidoRequest;
import com.comandadigital.order.dto.response.PedidoResponse;
import com.comandadigital.order.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping("/api/comandas/{comandaId}/pedidos")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PedidoResponse> enviar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long comandaId,
            @Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(201).body(pedidoService.enviarParaCozinha(restauranteId, comandaId, request));
    }

    @GetMapping("/api/pedidos/ativos")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PedidoResponse>> listarAtivos(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(pedidoService.listarAtivos(restauranteId));
    }

    @PutMapping("/api/pedidos/{id}/preparando")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PedidoResponse> preparando(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.marcarPreparando(restauranteId, id));
    }

    @PutMapping("/api/pedidos/{id}/pronto")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PedidoResponse> pronto(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.marcarPronto(restauranteId, id));
    }

    @PutMapping("/api/pedidos/{id}/entregue")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PedidoResponse> entregue(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.marcarEntregue(restauranteId, id));
    }
}
