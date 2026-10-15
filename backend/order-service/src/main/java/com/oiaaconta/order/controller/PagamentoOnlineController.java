package com.oiaaconta.order.controller;

import com.oiaaconta.order.service.MercadoPagoContaService;
import com.oiaaconta.order.service.PagamentoOnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Pagamento online no cardápio digital (Mercado Pago, conta do restaurante).
// /publico/** e /webhook são abertos (gateway + SecurityConfig): o cardápio
// não tem login — o acesso a um pedido é pelo token secreto dele.
@RestController
@RequestMapping("/api/pagamento-online")
@RequiredArgsConstructor
@Slf4j
public class PagamentoOnlineController {

    private final PagamentoOnlineService pagamentoOnlineService;
    private final MercadoPagoContaService contaService;

    // ─── Cardápio (público) ──────────────────────────────────────────────────

    @GetMapping("/publico/restaurantes/{restauranteId}")
    public ResponseEntity<Map<String, Object>> configPublica(@PathVariable Long restauranteId) {
        return ResponseEntity.ok(pagamentoOnlineService.configPublica(restauranteId));
    }

    @GetMapping("/publico/entregas/{entregaId}")
    public ResponseEntity<Map<String, Object>> resumo(@PathVariable Long entregaId, @RequestParam String token) {
        return ResponseEntity.ok(pagamentoOnlineService.resumo(entregaId, token));
    }

    @PostMapping("/publico/entregas/{entregaId}/pagamento")
    public ResponseEntity<Map<String, Object>> pagar(@PathVariable Long entregaId, @RequestParam String token,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(pagamentoOnlineService.criarPagamento(entregaId, token, body != null ? body : Map.of()));
    }

    @GetMapping("/publico/entregas/{entregaId}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable Long entregaId, @RequestParam String token) {
        return ResponseEntity.ok(pagamentoOnlineService.consultarStatus(entregaId, token));
    }

    // ─── Webhook (notification_url de cada pagamento) ────────────────────────

    // Sempre 200 pra notificações que não nos interessam (o MP para de
    // reenviar); exceção ao reconsultar o pagamento vira 500 e o MP retenta.
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam(name = "restaurante", required = false) Long restauranteId,
                                        @RequestParam(name = "data.id", required = false) String dataId,
                                        @RequestParam(name = "id", required = false) String idLegado,
                                        @RequestParam(name = "type", required = false) String type,
                                        @RequestParam(name = "topic", required = false) String topic,
                                        @RequestBody(required = false) Map<String, Object> body) {
        String tipo = type != null ? type : topic;
        String paymentId = dataId != null ? dataId : idLegado;
        if (body != null) {
            if (tipo == null && body.get("type") != null) tipo = body.get("type").toString();
            if (paymentId == null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
                paymentId = data.get("id").toString();
            }
        }
        if ("payment".equals(tipo)) {
            pagamentoOnlineService.processarWebhook(restauranteId, paymentId);
        }
        return ResponseEntity.ok().build();
    }

    // ─── Conexão da conta MP (painel do dono) ────────────────────────────────

    @GetMapping("/conexao")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> statusConexao(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(contaService.status(restauranteId));
    }

    @PostMapping("/conexao/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> iniciarConexao(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(Map.of("url", contaService.iniciarConexao(restauranteId)));
    }

    @PostMapping("/conexao/concluir")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> concluirConexao(@RequestHeader("X-Restaurante-Id") Long restauranteId,
                                                               @RequestBody Map<String, String> body) {
        contaService.concluirConexao(restauranteId, body.get("code"), body.get("state"));
        return ResponseEntity.ok(contaService.status(restauranteId));
    }

    @DeleteMapping("/conexao")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> desconectar(@RequestHeader("X-Restaurante-Id") Long restauranteId) {
        contaService.desconectar(restauranteId);
        return ResponseEntity.noContent().build();
    }
}
