package com.oiaaconta.order.controller;

import com.oiaaconta.order.dto.response.ComissaoResponse;
import com.oiaaconta.order.dto.response.ComparativoResponse;
import com.oiaaconta.order.dto.response.EvolucaoDiariaResponse;
import com.oiaaconta.order.dto.response.ResumoFinanceiroResponse;
import com.oiaaconta.order.dto.response.VendaResponse;
import com.oiaaconta.order.service.FinanceiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/financeiro")
@RequiredArgsConstructor
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final com.oiaaconta.order.repository.MetaFinanceiraRepository metaRepository;

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<ResumoFinanceiroResponse> resumo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getResumo(restauranteId, dataInicio, dataFim));
    }

    @GetMapping("/comissoes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ComissaoResponse>> comissoes(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getComissoes(restauranteId, dataInicio, dataFim));
    }

    @GetMapping("/evolucao")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<List<EvolucaoDiariaResponse>> evolucao(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getEvolucao(restauranteId, dataInicio, dataFim));
    }

    @GetMapping("/comparativo")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<ComparativoResponse> comparativo(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.getComparativo(restauranteId, dataInicio, dataFim));
    }

    // Vendas individuais do período (comandas fechadas + entregas com
    // pagamento confirmado) — mesma regra do resumo, pra conferir o
    // faturamento linha a linha na tela Financeiro.
    @GetMapping("/vendas")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<List<VendaResponse>> vendas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return ResponseEntity.ok(financeiroService.listarVendas(restauranteId, dataInicio, dataFim));
    }

    // ─── Metas do mês ─────────────────────────────────────────────────────

    @GetMapping("/metas")
    @PreAuthorize("hasAnyRole('ADMIN','CAIXA','SUPER_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> metas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestParam String mes) {
        validarMes(mes);
        var meta = metaRepository.findByRestauranteIdAndMes(restauranteId, mes).orElse(null);
        java.util.Map<String, Object> r = new java.util.HashMap<>();
        r.put("mes", mes);
        r.put("metaFaturamento", meta != null ? meta.getMetaFaturamento() : null);
        r.put("metaLucro", meta != null ? meta.getMetaLucro() : null);
        r.put("limiteDespesas", meta != null ? meta.getLimiteDespesas() : null);
        return ResponseEntity.ok(r);
    }

    @PutMapping("/metas")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<java.util.Map<String, Object>> salvarMetas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody java.util.Map<String, Object> body) {
        String mes = String.valueOf(body.get("mes"));
        validarMes(mes);
        var meta = metaRepository.findByRestauranteIdAndMes(restauranteId, mes)
            .orElseGet(() -> com.oiaaconta.order.entity.MetaFinanceira.builder().restauranteId(restauranteId).mes(mes).build());
        meta.setMetaFaturamento(valor(body.get("metaFaturamento")));
        meta.setMetaLucro(valor(body.get("metaLucro")));
        meta.setLimiteDespesas(valor(body.get("limiteDespesas")));
        meta.setAtualizadoEm(LocalDateTime.now());
        metaRepository.save(meta);
        return metas(restauranteId, mes);
    }

    private static void validarMes(String mes) {
        if (mes == null || !mes.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new com.oiaaconta.order.exception.BusinessException("Mês inválido (use AAAA-MM)");
        }
    }

    private static java.math.BigDecimal valor(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        java.math.BigDecimal b = new java.math.BigDecimal(v.toString());
        if (b.signum() < 0) throw new com.oiaaconta.order.exception.BusinessException("Meta não pode ser negativa");
        return b;
    }
}
