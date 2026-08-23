package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.ClienteRequest;
import com.oiaaconta.catalog.dto.response.ClienteResponse;
import com.oiaaconta.catalog.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivos) {
        return ResponseEntity.ok(clienteService.listar(restauranteId, apenasAtivos));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ClienteResponse> buscarPorTelefone(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam String telefone) {
        return ResponseEntity.ok(clienteService.buscarPorTelefone(restauranteId, telefone));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(restauranteId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ClienteResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(201).body(clienteService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ClienteResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(restauranteId, id, request));
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ClienteResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(clienteService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }
}
