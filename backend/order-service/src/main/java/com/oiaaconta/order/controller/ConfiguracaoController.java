package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.request.ConfiguracaoRequest;
import com.oiaaconta.order.dto.request.EncerramentoAntecipadoRequest;
import com.oiaaconta.order.dto.request.FreteConfigRequest;
import com.oiaaconta.order.dto.request.HorarioRequest;
import com.oiaaconta.order.dto.request.PausaProgramadaRequest;
import com.oiaaconta.order.dto.request.StatusLojaRequest;
import com.oiaaconta.order.dto.response.ConfiguracaoResponse;
import com.oiaaconta.order.dto.response.HorarioResponse;
import com.oiaaconta.order.dto.response.PausaResponse;
import com.oiaaconta.order.dto.response.StatusFuncionamentoResponse;
import com.oiaaconta.order.service.ConfiguracaoService;
import com.oiaaconta.order.service.HorarioFuncionamentoService;
import com.oiaaconta.order.service.PausaFuncionamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;
    private final PausaFuncionamentoService pausaFuncionamentoService;
    private final HorarioFuncionamentoService horarioFuncionamentoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> get(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(configuracaoService.get(restauranteId));
    }

    @PutMapping("/alerta-pedido")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> atualizarAlertaPedido(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody ConfiguracaoRequest request) {
        return ResponseEntity.ok(configuracaoService.upsert(restauranteId, request));
    }

    @PutMapping("/pix")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> atualizarPix(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody ConfiguracaoRequest request) {
        return ResponseEntity.ok(configuracaoService.upsert(restauranteId, request));
    }

    @PutMapping("/comissoes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> atualizarComissoes(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody ConfiguracaoRequest request) {
        return ResponseEntity.ok(configuracaoService.upsert(restauranteId, request));
    }

    @PutMapping("/frete")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> atualizarFrete(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody FreteConfigRequest request) {
        return ResponseEntity.ok(configuracaoService.atualizarFrete(restauranteId, request));
    }

    @PutMapping("/status-loja")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConfiguracaoResponse> atualizarStatusLoja(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody StatusLojaRequest request) {
        return ResponseEntity.ok(configuracaoService.atualizarStatusLoja(restauranteId, request));
    }

    @PostMapping("/pausas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PausaResponse> criarPausa(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody PausaProgramadaRequest request) {
        return ResponseEntity.status(201).body(
            pausaFuncionamentoService.criarProgramada(restauranteId, request.getTitulo(), request.getInicio(), request.getFim()));
    }

    @PostMapping("/pausas/encerramento-antecipado")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PausaResponse> criarEncerramentoAntecipado(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody EncerramentoAntecipadoRequest request) {
        return ResponseEntity.status(201).body(
            pausaFuncionamentoService.criarEncerramentoAntecipado(restauranteId, request.getMotivo()));
    }

    @GetMapping("/pausas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PausaResponse>> listarPausas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(pausaFuncionamentoService.listar(restauranteId));
    }

    @DeleteMapping("/pausas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> cancelarPausa(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable Long id) {
        pausaFuncionamentoService.cancelar(restauranteId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pausas/status")
    public ResponseEntity<StatusFuncionamentoResponse> statusPausa(
            @RequestParam Long restauranteId) {
        return ResponseEntity.ok(pausaFuncionamentoService.getStatus(restauranteId));
    }

    @GetMapping("/horarios")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<HorarioResponse>> listarHorarios(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(horarioFuncionamentoService.listar(restauranteId));
    }

    @PutMapping("/horarios")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<HorarioResponse>> salvarHorarios(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @Valid @RequestBody List<HorarioRequest> horarios) {
        return ResponseEntity.ok(horarioFuncionamentoService.salvarSemana(restauranteId, horarios));
    }
}
