package com.oiaaconta.billing.controller;

import com.oiaaconta.billing.entity.Contrato;
import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.enums.ModalidadeOperacao;
import com.oiaaconta.billing.enums.StatusContrato;
import com.oiaaconta.billing.exception.LimiteTrocasModalidadeExcedidoException;
import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<Contrato>> listar(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(billingService.listarContratos(pageable));
    }

    @GetMapping("/meu")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Contrato> meuContrato(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarContratoDoRestaurante(restauranteId));
    }

    @GetMapping("/restaurante/{restauranteId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> buscarPorRestaurante(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(billingService.buscarContratoDoRestaurante(restauranteId));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> criar(@RequestBody Map<String, Object> body) {
        Long restauranteId = ((Number) body.get("restauranteId")).longValue();
        Long planoId = ((Number) body.get("planoId")).longValue();
        ModalidadeOperacao modalidade = body.get("modalidadeOperacao") != null
            ? ModalidadeOperacao.valueOf(body.get("modalidadeOperacao").toString())
            : null;
        return ResponseEntity.status(201).body(billingService.criarContrato(restauranteId, planoId, modalidade));
    }

    // Troca de modalidade pedida pelo dono via suporte — só o SUPER_ADMIN
    // (equipe de suporte) pode fazer essa alteração, o dono não tem esse
    // controle direto no painel.
    @PutMapping("/{id}/modalidade")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> atualizarModalidade(@PathVariable Long id,
                                                         @RequestBody Map<String, String> body) {
        ModalidadeOperacao modalidade = ModalidadeOperacao.valueOf(body.get("modalidadeOperacao"));
        return ResponseEntity.ok(billingService.atualizarModalidadeOperacao(id, modalidade));
    }

    // Troca de modalidade feita pelo próprio dono, direto no painel — só
    // enquanto ainda tiver trocas gratuitas (ver BillingService.alterarMinhaModalidade).
    @PutMapping("/meu/modalidade")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Contrato> alterarMinhaModalidade(@RequestHeader("X-Restaurante-Id") Long restauranteId,
                                                            @RequestBody Map<String, String> body) {
        ModalidadeOperacao modalidade = ModalidadeOperacao.valueOf(body.get("modalidadeOperacao"));
        return ResponseEntity.ok(billingService.alterarMinhaModalidade(restauranteId, modalidade));
    }

    // Troca de plano feita pelo próprio dono, direto no painel.
    @PutMapping("/meu/plano")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Contrato> alterarMeuPlano(@RequestHeader("X-Restaurante-Id") Long restauranteId,
                                                     @RequestBody Map<String, Object> body) {
        Long planoId = ((Number) body.get("planoId")).longValue();
        ModalidadeOperacao modalidade = body.get("modalidadeOperacao") != null
            ? ModalidadeOperacao.valueOf(body.get("modalidadeOperacao").toString())
            : null;
        return ResponseEntity.ok(billingService.alterarMeuPlano(restauranteId, planoId, modalidade));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Contrato> atualizarStatus(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        StatusContrato status = StatusContrato.valueOf(body.get("status"));
        return ResponseEntity.ok(billingService.atualizarStatusContrato(id, status));
    }

    @PostMapping("/{id}/pagamento-manual")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Pagamento> pagamentoManual(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> body) {
        BigDecimal valor = new BigDecimal(body.get("valor").toString());
        String obs = body.getOrDefault("observacao", "").toString();
        return ResponseEntity.status(201).body(billingService.registrarPagamentoManual(id, valor, obs));
    }

    @GetMapping("/{id}/pagamentos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<Pagamento>> pagamentos(@PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(billingService.listarPagamentosDoContrato(id, pageable));
    }

    // Restaurante sem contrato (ex: cadastrado fora do fluxo normal de
    // registro) — resposta limpa de "não encontrado" em vez de deixar a
    // exceção não tratada propagar como erro genérico.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNaoEncontrado() {
        return ResponseEntity.notFound().build();
    }

    // Dono tentou trocar modalidade sozinho depois de esgotar as trocas
    // gratuitas — devolve a mensagem explicando o valor/como proceder pro
    // frontend exibir direto pro usuário.
    @ExceptionHandler(LimiteTrocasModalidadeExcedidoException.class)
    public ResponseEntity<Map<String, String>> handleLimiteTrocasExcedido(LimiteTrocasModalidadeExcedidoException ex) {
        return ResponseEntity.status(403).body(Map.of("mensagem", ex.getMessage()));
    }
}
