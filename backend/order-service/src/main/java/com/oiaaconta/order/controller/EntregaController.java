package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.EntregaRequest;
import com.oiaaconta.order.dto.request.FretePreviewRequest;
import com.oiaaconta.order.dto.response.EntregaResponse;
import com.oiaaconta.order.dto.response.RotaSugeridaResponse;
import com.oiaaconta.order.service.EntregaService;
import com.oiaaconta.order.service.FreteService;
import com.oiaaconta.order.service.RotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;
    private final RotaService rotaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> criar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.status(201).body(entregaService.criar(restauranteId, request));
    }

    @PostMapping("/frete-preview")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<FreteService.FreteCalculado> fretePreview(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody FretePreviewRequest request) {
        return ResponseEntity.ok(entregaService.calcularFretePreview(
            restauranteId, request.getEnderecoLatitude(), request.getEnderecoLongitude()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN','ENTREGADOR')")
    public ResponseEntity<Page<EntregaResponse>> listar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(entregaService.listar(restauranteId, pageable));
    }

    @GetMapping("/aguardando")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<EntregaResponse>> aguardando(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(entregaService.listarAguardando(restauranteId, pageable));
    }

    @GetMapping("/confirmadas")
    @PreAuthorize("hasAnyRole('ENTREGADOR','GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<EntregaResponse>> confirmadas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(entregaService.listarConfirmadas(restauranteId, pageable));
    }

    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> confirmar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.confirmar(restauranteId, id));
    }

    @PutMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('COZINHA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> rejeitar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(entregaService.rejeitar(restauranteId, id, body.get("motivo")));
    }

    @PutMapping("/{id}/atribuir-entregador")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> atribuirEntregador(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long entregadorId,
            @RequestHeader("X-User-Nome") String entregadorNome,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.atribuirEntregador(restauranteId, id, entregadorId, entregadorNome));
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
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Nome") String userNome,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.saiu(restauranteId, id, userId, userNome));
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

    @GetMapping("/em-rota")
    @PreAuthorize("hasAnyRole('GARCON','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<EntregaResponse>> emRota(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(entregaService.listarEmRota(restauranteId));
    }

    @PutMapping("/{id}/localizacao")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> atualizarLocalizacao(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long usuarioId,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Double> body,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        entregaService.atualizarLocalizacao(restauranteId, id, usuarioId, isAdmin, body.get("latitude"), body.get("longitude"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/minha-rota")
    @PreAuthorize("hasAnyRole('ENTREGADOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RotaSugeridaResponse> minhaRota(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestHeader("X-User-Id") Long entregadorId,
            @RequestParam Double lat,
            @RequestParam Double lng) {
        return ResponseEntity.ok(rotaService.sugerir(restauranteId, entregadorId, lat, lng));
    }

    @GetMapping("/pendentes-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<EntregaResponse>> pendentesPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(entregaService.listarPendentesPagamento(restauranteId, pageable));
    }

    @PutMapping("/{id}/confirmar-pagamento")
    @PreAuthorize("hasAnyRole('CAIXA','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EntregaResponse> confirmarPagamento(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        return ResponseEntity.ok(entregaService.confirmarPagamento(restauranteId, id));
    }
}
