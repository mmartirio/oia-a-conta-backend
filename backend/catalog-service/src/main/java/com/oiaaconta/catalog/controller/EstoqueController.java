package com.oiaaconta.catalog.controller;

import com.oiaaconta.catalog.dto.request.EstoqueBaixaRequest;
import com.oiaaconta.catalog.dto.request.EstoqueConfigRequest;
import com.oiaaconta.catalog.dto.request.MovimentoEstoqueRequest;
import com.oiaaconta.catalog.dto.response.EstoqueResponse;
import com.oiaaconta.catalog.dto.response.MovimentacaoEstoqueResponse;
import com.oiaaconta.catalog.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public ResponseEntity<List<EstoqueResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(estoqueService.listar(restauranteId));
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<EstoqueResponse>> alertas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(estoqueService.alertas(restauranteId));
    }

    @GetMapping("/{produtoId}")
    public ResponseEntity<EstoqueResponse> buscarPorProduto(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long produtoId) {
        return ResponseEntity.ok(estoqueService.buscarPorProduto(restauranteId, produtoId));
    }

    @PutMapping("/{produtoId}/config")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EstoqueResponse> configurar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long produtoId,
            @Valid @RequestBody EstoqueConfigRequest request) {
        return ResponseEntity.ok(estoqueService.configurar(restauranteId, produtoId, request));
    }

    @PostMapping("/{produtoId}/movimentar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EstoqueResponse> movimentar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader(value = "X-User-Id", required = false) Long usuarioId,
            @RequestHeader(value = "X-User-Nome", required = false) String usuarioNome,
            @PathVariable Long produtoId,
            @Valid @RequestBody MovimentoEstoqueRequest request) {
        return ResponseEntity.ok(estoqueService.movimentar(restauranteId, produtoId, request, usuarioId, usuarioNome));
    }

    @GetMapping("/{produtoId}/movimentacoes")
    public ResponseEntity<Page<MovimentacaoEstoqueResponse>> listarMovimentacoes(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long produtoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(estoqueService.listarMovimentacoes(restauranteId, produtoId, pageable));
    }

    // Endpoints internos consumidos pelo order-service (Feign) no momento da venda —
    // sem @PreAuthorize específico de role de admin (chamada service-to-service,
    // autenticada via o mesmo JWT repassado da requisição original).
    @PostMapping("/verificar-baixar")
    public ResponseEntity<Void> verificarEBaixar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EstoqueBaixaRequest request) {
        estoqueService.verificarEBaixar(restauranteId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/liberar")
    public ResponseEntity<Void> liberar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EstoqueBaixaRequest request) {
        estoqueService.liberar(restauranteId, request);
        return ResponseEntity.ok().build();
    }
}
