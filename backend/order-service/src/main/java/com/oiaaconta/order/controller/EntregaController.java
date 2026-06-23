package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.EntregaRequest;
import com.oiaaconta.order.dto.response.EntregaResponse;
import com.oiaaconta.order.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.status(201).body(entregaService.criar(restauranteId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN','ENTREGADOR')")
    public ResponseEntity<List<EntregaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(entregaService.listar(restauranteId));
    }

    @GetMapping("/aguardando")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EntregaResponse>> aguardando(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(entregaService.listarAguardando(restauranteId));
    }

    @PutMapping("/{id}/aceitar")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> aceitar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long entregadorId,
            @RequestHeader("X-User-Nome") String entregadorNome,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.aceitar(restauranteId, id, entregadorId, entregadorNome));
    }

    @PutMapping("/{id}/pronto")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> pronto(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.prontoParaEntrega(restauranteId, id));
    }

    @PutMapping("/{id}/saiu")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> saiu(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.saiu(restauranteId, id));
    }

    @PutMapping("/{id}/entregue")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> entregue(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.entregar(restauranteId, id));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> cancelar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.cancelar(restauranteId, id));
    }

    @GetMapping("/pendentes-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EntregaResponse>> pendentesPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(entregaService.listarPendentesPagamento(restauranteId));
    }

    @PutMapping("/{id}/confirmar-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> confirmarPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.confirmarPagamento(restauranteId, id));
    }
}
