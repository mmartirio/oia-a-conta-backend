package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.dto.request.UsuarioRequest;
import com.oiaaconta.auth.dto.response.UsuarioResponse;
import com.oiaaconta.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId) {
        return ResponseEntity.ok(usuarioService.listarPorRestaurante(restauranteId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> criar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(201).body(usuarioService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(restauranteId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> desativar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @PathVariable @NonNull Long id) {
        usuarioService.desativar(restauranteId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarAdmins() {
        return ResponseEntity.ok(usuarioService.listarAdminsGlobal());
    }

    @GetMapping("/restaurante/{restauranteId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarPorRestauranteGlobal(
            @PathVariable @NonNull Long restauranteId) {
        return ResponseEntity.ok(usuarioService.listarPorRestauranteGlobal(restauranteId));
    }

    @PostMapping("/{id}/reset-senha")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> resetSenha(@PathVariable @NonNull Long id) {
        String novaSenha = usuarioService.resetSenha(id);
        return ResponseEntity.ok(java.util.Map.of(
            "mensagem", "Senha redefinida com sucesso",
            "novaSenha", novaSenha
        ));
    }

    @GetMapping("/super-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarSuperAdmins() {
        return ResponseEntity.ok(usuarioService.listarSuperAdmins());
    }

    @PostMapping("/super-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> criarSuperAdmin(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(201).body(usuarioService.criarSuperAdmin(request));
    }

    @PutMapping("/super-admins/{id}/ativo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponse> alternarAtivoSuperAdmin(
            @PathVariable @NonNull Long id,
            @RequestBody java.util.Map<String, Boolean> body) {
        return ResponseEntity.ok(usuarioService.alternarAtivoSuperAdmin(id, Boolean.TRUE.equals(body.get("ativo"))));
    }
}
