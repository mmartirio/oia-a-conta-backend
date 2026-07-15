package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.dto.request.GrupoRequest;
import com.oiaaconta.auth.dto.response.GrupoResponse;
import com.oiaaconta.auth.service.GrupoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class GrupoController {

    private final GrupoService grupoService;

    @GetMapping
    public ResponseEntity<List<GrupoResponse>> listar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId) {
        return ResponseEntity.ok(grupoService.listar(restauranteId));
    }

    @PostMapping
    public ResponseEntity<GrupoResponse> criar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @Valid @RequestBody GrupoRequest request) {
        return ResponseEntity.status(201).body(grupoService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @PathVariable @NonNull Long id,
            @Valid @RequestBody GrupoRequest request) {
        return ResponseEntity.ok(grupoService.atualizar(restauranteId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> excluir(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @PathVariable @NonNull Long id) {
        long usuariosDesatribuidos = grupoService.excluir(restauranteId, id);
        return ResponseEntity.ok(Map.of("usuariosDesatribuidos", usuariosDesatribuidos));
    }
}
