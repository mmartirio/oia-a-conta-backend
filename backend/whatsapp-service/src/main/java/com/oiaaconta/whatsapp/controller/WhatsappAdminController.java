package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.dto.MensagemTemplateDto;
import com.oiaaconta.whatsapp.dto.WhatsappStatusDto;
import com.oiaaconta.whatsapp.dto.response.ConversaResumoResponse;
import com.oiaaconta.whatsapp.dto.response.MensagemResponse;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import com.oiaaconta.whatsapp.service.MensagemTemplateService;
import com.oiaaconta.whatsapp.service.MensagemWhatsappService;
import com.oiaaconta.whatsapp.service.WhatsappAdminService;
import com.oiaaconta.whatsapp.service.WhatsappConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class WhatsappAdminController {

    private final WhatsappAdminService adminService;
    private final MensagemTemplateService mensagemService;
    private final MensagemWhatsappService mensagemWhatsappService;
    private final WhatsappConfigService whatsappConfigService;
    private final SessaoWhatsappRepository sessaoRepo;

    @GetMapping("/status")
    public ResponseEntity<WhatsappStatusDto> status(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(adminService.status(restauranteId));
    }

    @PostMapping("/conectar")
    public ResponseEntity<WhatsappStatusDto> conectar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(adminService.conectar(restauranteId));
    }

    @DeleteMapping("/desconectar")
    public ResponseEntity<Void> desconectar(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        adminService.desconectar(restauranteId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mensagens")
    public ResponseEntity<List<MensagemTemplateDto>> mensagens(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(mensagemService.listar(restauranteId));
    }

    @PostMapping("/mensagens")
    public ResponseEntity<MensagemTemplateDto> criarMensagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody Map<String, String> body) {
        MensagemTemplateDto dto = mensagemService.criar(
            restauranteId,
            body.get("label"),
            body.get("texto"),
            body.get("grupo")
        );
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/mensagens/{chave}")
    public ResponseEntity<Void> salvarMensagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String chave,
            @RequestBody Map<String, String> body) {
        mensagemService.salvar(restauranteId, chave, body.get("texto"), body.get("label"));
        return ResponseEntity.ok().build();
    }

    // Remove mensagem: sistema → desativa (ativo=false); custom → exclui do banco
    @DeleteMapping("/mensagens/{chave}")
    public ResponseEntity<Void> removerMensagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String chave) {
        mensagemService.remover(restauranteId, chave);
        return ResponseEntity.ok().build();
    }

    // Restaura mensagem de sistema ao texto padrão (reativa se estava desativada)
    @PostMapping("/mensagens/{chave}/restaurar")
    public ResponseEntity<Void> restaurarMensagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String chave) {
        mensagemService.restaurarOuExcluir(restauranteId, chave);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mensagens/ordem")
    public ResponseEntity<Void> salvarOrdem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody List<Map<String, Object>> itens) {
        mensagemService.salvarOrdem(restauranteId, itens);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessoes/pausadas")
    public ResponseEntity<List<Map<String, Object>>> sessoesPausadas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        List<Map<String, Object>> resultado = sessaoRepo
            .findByRestauranteIdAndEstado(restauranteId, EstadoSessao.PAUSADO)
            .stream()
            .map(s -> Map.<String, Object>of(
                "telefone", s.getTelefone(),
                "clienteNome", s.getClienteNome() != null ? s.getClienteNome() : "",
                "pausadoEm", s.getUltimaInteracao() != null ? s.getUltimaInteracao().toString() : ""
            ))
            .toList();
        return ResponseEntity.ok(resultado);
    }

    @Transactional
    @PostMapping("/sessoes/{telefone}/retomar")
    public ResponseEntity<Void> retomarSessao(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String telefone) {
        sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .ifPresent(s -> {
                s.setEstado(EstadoSessao.INICIO);
                sessaoRepo.save(s);
            });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/chatbot-status")
    public ResponseEntity<Map<String, Boolean>> chatbotStatus(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(Map.of("ativo", whatsappConfigService.isChatbotAtivo(restauranteId)));
    }

    @PutMapping("/chatbot-status")
    public ResponseEntity<Map<String, Boolean>> atualizarChatbotStatus(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody Map<String, Boolean> body) {
        boolean ativo = whatsappConfigService.atualizarChatbotAtivo(restauranteId, Boolean.TRUE.equals(body.get("ativo")));
        return ResponseEntity.ok(Map.of("ativo", ativo));
    }

    // Imagem do cardápio numerado (desenhada pelo admin) enviada pelo lembrete
    // de 10 min do chatbot — data URI base64, mesmo padrão de imagem de
    // produto no catalog-service. body.imagemBase64 vazio/null remove a imagem.
    @GetMapping("/cardapio-imagem")
    public ResponseEntity<Map<String, String>> getCardapioImagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(Map.of("imagemBase64",
            java.util.Objects.requireNonNullElse(whatsappConfigService.getImagemCardapio(restauranteId), "")));
    }

    @PutMapping("/cardapio-imagem")
    public ResponseEntity<Void> atualizarCardapioImagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @RequestBody Map<String, String> body) {
        whatsappConfigService.atualizarImagemCardapio(restauranteId, body.get("imagemBase64"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversas")
    public ResponseEntity<List<ConversaResumoResponse>> conversas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(mensagemWhatsappService.listarConversas(restauranteId));
    }

    @GetMapping("/conversas/contagem-nao-lidas")
    public ResponseEntity<Map<String, Long>> contarConversasNaoLidas(
            @RequestHeader("X-Restaurante-Id") Long restauranteId) {
        return ResponseEntity.ok(Map.of("quantidade", mensagemWhatsappService.contarConversasNaoLidas(restauranteId)));
    }

    @GetMapping("/conversas/{telefone}/mensagens")
    public ResponseEntity<Page<MensagemResponse>> mensagensDaConversa(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String telefone,
            Pageable pageable) {
        return ResponseEntity.ok(mensagemWhatsappService.listarMensagens(restauranteId, telefone, pageable));
    }

    @PutMapping("/conversas/{telefone}/nome")
    public ResponseEntity<Void> renomearCliente(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String telefone,
            @RequestBody Map<String, String> body) {
        mensagemWhatsappService.renomearCliente(restauranteId, telefone, body.get("nome"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversas/{telefone}/mensagens")
    public ResponseEntity<Void> enviarMensagem(
            @RequestHeader("X-Restaurante-Id") Long restauranteId,
            @PathVariable String telefone,
            @RequestBody Map<String, String> body) {
        String texto = body.get("texto");
        if (texto == null || texto.isBlank()) return ResponseEntity.badRequest().build();
        adminService.enviarMensagemTexto(restauranteId, telefone, texto);
        mensagemWhatsappService.registrar(restauranteId, telefone, DirecaoMensagem.ENVIADA, texto);
        return ResponseEntity.ok().build();
    }
}
