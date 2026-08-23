package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.GrupoClienteMembroRequest;
import com.oiaaconta.catalog.dto.request.GrupoClienteRequest;
import com.oiaaconta.catalog.dto.response.GrupoClienteMembroResponse;
import com.oiaaconta.catalog.dto.response.GrupoClienteResponse;
import com.oiaaconta.catalog.service.GrupoClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos-clientes")
@RequiredArgsConstructor
public class GrupoClienteController {

    private final GrupoClienteService grupoClienteService;

    @GetMapping
    public ResponseEntity<List<GrupoClienteResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        return ResponseEntity.ok(grupoClienteService.listar(restauranteId, apenasAtivos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoClienteResponse> buscarPorId(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(grupoClienteService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GrupoClienteResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody GrupoClienteRequest request) {
        return ResponseEntity.status(201).body(grupoClienteService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GrupoClienteResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody GrupoClienteRequest request) {
        return ResponseEntity.ok(grupoClienteService.atualizar(restauranteId, id, request));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<GrupoClienteResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(grupoClienteService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }

    @GetMapping("/{id}/membros")
    public ResponseEntity<List<GrupoClienteMembroResponse>> listarMembros(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(grupoClienteService.listarMembros(restauranteId, id));
    }

    @PostMapping("/{id}/membros")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> adicionarMembro(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody GrupoClienteMembroRequest request) {
        grupoClienteService.adicionarMembro(restauranteId, id, request.getClienteId());
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}/membros/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> removerMembro(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @PathVariable Long clienteId) {
        grupoClienteService.removerMembro(restauranteId, id, clienteId);
        return ResponseEntity.noContent().build();
    }
}
