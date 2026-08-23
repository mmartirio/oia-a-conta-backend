package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.AtivoRequest;
import com.oiaaconta.catalog.dto.request.CategoriaRequest;
import com.oiaaconta.catalog.dto.request.ReordenarCategoriasRequest;
import com.oiaaconta.catalog.dto.response.CategoriaResponse;
import com.oiaaconta.catalog.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam(required = false, defaultValue = "false") boolean incluirInativos) {
        return ResponseEntity.ok(categoriaService.listar(restauranteId, incluirInativos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CategoriaResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(201).body(categoriaService.criar(restauranteId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CategoriaResponse> atualizar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.atualizar(restauranteId, id, request));
    }

    @PutMapping("/reordenar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CategoriaResponse>> reordenar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody ReordenarCategoriasRequest request) {
        return ResponseEntity.ok(categoriaService.reordenar(restauranteId, request.getIds()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> desativar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        categoriaService.desativar(restauranteId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CategoriaResponse> alterarAtivo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @Valid @RequestBody AtivoRequest request) {
        return ResponseEntity.ok(categoriaService.alterarAtivo(restauranteId, id, request.getAtivo()));
    }
}
