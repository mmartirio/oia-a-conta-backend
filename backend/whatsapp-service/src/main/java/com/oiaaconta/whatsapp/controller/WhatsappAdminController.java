package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.dto.MensagemTemplateDto;
import com.oiaaconta.whatsapp.dto.WhatsappStatusDto;
import com.oiaaconta.whatsapp.service.MensagemTemplateService;
import com.oiaaconta.whatsapp.service.WhatsappAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
        mensagemService.salvar(restauranteId, chave, body.get("texto"));
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
}
