package com.oiaaconta.table.controller;

import com.oiaaconta.table.dto.request.MesaRequest;
import com.oiaaconta.table.dto.response.MesaResponse;
import com.oiaaconta.table.enums.StatusMesa;
import com.oiaaconta.table.service.MesaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;

    @GetMapping
    public ResponseEntity<List<MesaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(mesaService.listar(restauranteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MesaResponse> buscar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(mesaService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MesaResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody MesaRequest request) {
        return ResponseEntity.status(201).body(mesaService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MesaResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody MesaRequest request) {
        return ResponseEntity.ok(mesaService.atualizar(restauranteId, id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GARCON','COZINHA','SUPER_ADMIN')")
    public ResponseEntity<MesaResponse> atualizarStatus(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        StatusMesa status = StatusMesa.valueOf(body.get("status"));
        return ResponseEntity.ok(mesaService.atualizarStatus(restauranteId, id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> excluir(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        mesaService.excluir(restauranteId, id);
        return ResponseEntity.noContent().build();
    }
}
