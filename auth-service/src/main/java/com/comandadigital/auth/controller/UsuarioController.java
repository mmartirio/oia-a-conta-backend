package com.comandadigital.auth.controller;

import com.comandadigital.auth.dto.request.UsuarioRequest;
import com.comandadigital.auth.dto.response.UsuarioResponse;
import com.comandadigital.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(usuarioService.listarPorRestaurante(restauranteId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(201).body(usuarioService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(restauranteId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> desativar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        usuarioService.desativar(restauranteId, id);
        return ResponseEntity.noContent().build();
    }
}
