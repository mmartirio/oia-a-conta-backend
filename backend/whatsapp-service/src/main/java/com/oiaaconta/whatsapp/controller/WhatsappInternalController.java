package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.service.MensagemTemplateService;
import com.oiaaconta.whatsapp.service.NotificacaoService;
import com.oiaaconta.whatsapp.service.WhatsappAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/whatsapp")
@RequiredArgsConstructor
public class WhatsappInternalController {

    private final NotificacaoService notificacaoService;
    private final MensagemTemplateService mensagemService;
    private final WhatsappAdminService whatsappAdminService;

    // Chamado pelo billing-service quando o suporte (SUPER_ADMIN) responde um
    // ticket originado do número de WhatsApp da plataforma.
    @PostMapping("/enviar-suporte")
    public ResponseEntity<Void> enviarSuporte(@RequestBody Map<String, String> body) {
        whatsappAdminService.enviarMensagemTexto(null, body.get("telefone"), body.get("mensagem"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notificar")
    public ResponseEntity<Void> notificar(@RequestBody NotificarRequest request) {
        String texto;
        if (request.getTipo() != null && request.getRestauranteId() != null) {
            texto = mensagemService.resolverTexto(
                request.getRestauranteId(), request.getTipo(), request.getVariaveis());
        } else {
            texto = request.getMensagem();
        }
        if (texto != null && !texto.isBlank()) {
            notificacaoService.notificarPorEntregaId(request.getEntregaId(), texto);
        }
        return ResponseEntity.ok().build();
    }

    @Data
    public static class NotificarRequest {
        private Long entregaId;
        private String mensagem;
        private String tipo;
        private Long restauranteId;
        private Map<String, String> variaveis;
    }
}
