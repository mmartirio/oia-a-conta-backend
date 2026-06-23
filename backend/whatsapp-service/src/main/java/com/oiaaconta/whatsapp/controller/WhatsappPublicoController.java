package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.dto.PedidoPublicoRequest;
import com.oiaaconta.whatsapp.entity.ItemCarrinho;
import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import com.oiaaconta.whatsapp.service.MensagemTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp/publico")
@RequiredArgsConstructor
@Slf4j
public class WhatsappPublicoController {

    private final SessaoWhatsappRepository sessaoRepo;
    private final EvolutionApiClient evolutionClient;
    private final MensagemTemplateService mensagemService;

    @PostMapping("/{restauranteId}/pedido")
    @Transactional
    public ResponseEntity<Map<String, Object>> criarPedidoPublico(
            @PathVariable Long restauranteId,
            @RequestBody PedidoPublicoRequest request) {

        if (request.getTelefone() == null || request.getTelefone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Telefone é obrigatório"));
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum item no pedido"));
        }

        String telefone = normalizarTelefone(request.getTelefone());

        SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .orElseGet(() -> SessaoWhatsapp.builder()
                .telefone(telefone)
                .restauranteId(restauranteId)
                .build());

        sessao.setClienteNome(request.getClienteNome());
        sessao.setEstado(EstadoSessao.COLETANDO_ENDERECO);
        sessao.setEntregaId(null);
        sessao.setErrosConsecutivos(0);
        sessao.setMetodoPagamento(null);
        sessao.setObservacao(null);
        sessao.setEnderecoRua(null);
        sessao.setEnderecoNumero(null);
        sessao.setEnderecoBairro(null);
        sessao.setEnderecoCidade(null);
        sessao.setEnderecoComplemento(null);

        List<ItemCarrinho> itens = new ArrayList<>();
        for (PedidoPublicoRequest.ItemDto dto : request.getItens()) {
            itens.add(ItemCarrinho.builder()
                .sessao(sessao)
                .produtoId(dto.getProdutoId())
                .produtoNome(dto.getProdutoNome())
                .precoUnitario(dto.getPrecoUnitario())
                .quantidade(dto.getQuantidade())
                .build());
        }

        sessao.getItens().clear();
        sessao.getItens().addAll(itens);

        sessaoRepo.save(sessao);

        enviarMensagemBoasVindas(telefone, restauranteId, request, itens);

        return ResponseEntity.ok(Map.of(
            "mensagem", "Pedido recebido! Continue a conversa no WhatsApp para finalizar a entrega.",
            "telefone", telefone
        ));
    }

    private void enviarMensagemBoasVindas(String telefone, Long restauranteId,
            PedidoPublicoRequest request, List<ItemCarrinho> itens) {
        try {
            StringBuilder sb = new StringBuilder();
            String nome = request.getClienteNome() != null ? request.getClienteNome() : "Cliente";
            sb.append("Olá, *").append(nome).append("*! 🎉\n\n");
            sb.append("Recebemos seu pedido pelo cardápio digital:\n\n");

            BigDecimal total = BigDecimal.ZERO;
            for (ItemCarrinho item : itens) {
                BigDecimal subtotal = item.getPrecoUnitario().multiply(
                    BigDecimal.valueOf(item.getQuantidade()));
                total = total.add(subtotal);
                sb.append("• ").append(item.getProdutoNome())
                  .append(" x").append(item.getQuantidade())
                  .append(" — R$ ").append(String.format("%.2f", subtotal).replace('.', ','))
                  .append("\n");
            }

            sb.append("\n*Total: R$ ").append(String.format("%.2f", total).replace('.', ',')).append("*\n\n");

            String msgEndereco = mensagemService.resolverTexto(restauranteId, "CHATBOT_PEDIR_ENDERECO", null);
            if (msgEndereco == null || msgEndereco.isBlank()) {
                sb.append("Para finalizar a entrega, informe seu endereço completo (rua, número, bairro e cidade):");
            } else {
                sb.append(msgEndereco);
            }

            evolutionClient.enviarMensagem(telefone, sb.toString());
        } catch (Exception e) {
            log.warn("Erro ao enviar mensagem WhatsApp para pedido público: {}", e.getMessage());
        }
    }

    private String normalizarTelefone(String telefone) {
        String digits = telefone.replaceAll("[^0-9]", "");
        if (!digits.startsWith("55") && digits.length() <= 11) {
            digits = "55" + digits;
        }
        return digits;
    }
}
