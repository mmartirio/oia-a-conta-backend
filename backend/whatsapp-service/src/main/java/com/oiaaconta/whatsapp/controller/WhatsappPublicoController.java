package com.oiaaconta.whatsapp.controller;

import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.client.OrderClient;
import com.oiaaconta.whatsapp.dto.PedidoPublicoRequest;
import com.oiaaconta.whatsapp.entity.ItemCarrinho;
import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import com.oiaaconta.whatsapp.enums.EstadoSessao;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import com.oiaaconta.whatsapp.service.MensagemTemplateService;
import com.oiaaconta.whatsapp.service.MensagemWhatsappService;
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
    private final MensagemWhatsappService mensagemWhatsappService;
    private final OrderClient orderClient;

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
        if (request.getEndereco() == null || request.getEndereco().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Endereço é obrigatório"));
        }
        if (request.getMetodoPagamento() == null || request.getMetodoPagamento().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Forma de pagamento é obrigatória"));
        }

        String telefone = normalizarTelefone(request.getTelefone());

        SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .orElseGet(() -> SessaoWhatsapp.builder()
                .telefone(telefone)
                .restauranteId(restauranteId)
                .build());

        sessao.setClienteNome(request.getClienteNome());
        sessao.setErrosConsecutivos(0);
        sessao.setEnderecoRua(request.getEndereco().trim());
        sessao.setEnderecoNumero(null);
        sessao.setEnderecoBairro(null);
        sessao.setEnderecoCidade(null);
        sessao.setEnderecoComplemento(null);
        sessao.setMetodoPagamento(request.getMetodoPagamento());
        sessao.setObservacao(request.getObservacao());

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

        List<OrderClient.ItemEntregaRequest> itensRequest = itens.stream()
            .map(i -> OrderClient.ItemEntregaRequest.builder()
                .produtoId(i.getProdutoId())
                .produtoNome(i.getProdutoNome())
                .precoUnitario(i.getPrecoUnitario())
                .quantidade(i.getQuantidade())
                .build())
            .toList();

        OrderClient.EntregaResponse entrega;
        try {
            entrega = orderClient.criarEntrega(restauranteId, OrderClient.EntregaRequest.builder()
                .clienteNome(request.getClienteNome())
                .clienteTelefone(telefone)
                .enderecoRua(request.getEndereco().trim())
                .metodoPagamento(request.getMetodoPagamento())
                .observacao(request.getObservacao())
                .origemWhatsapp(true)
                .itens(itensRequest)
                .build());
        } catch (Exception e) {
            log.error("Erro ao criar entrega via cardápio público: {}", e.getMessage(), e);
            return ResponseEntity.status(502).body(Map.of("erro", "Não foi possível enviar o pedido. Tente novamente."));
        }

        sessao.setEntregaId(entrega.getId());
        sessao.setEstado("PIX".equals(request.getMetodoPagamento()) ? EstadoSessao.AGUARDANDO_PIX : EstadoSessao.PEDIDO_ENVIADO);
        sessaoRepo.save(sessao);

        enviarMensagemConfirmacao(telefone, restauranteId, request, itens, entrega.getId());

        return ResponseEntity.ok(Map.of(
            "mensagem", "Pedido enviado com sucesso!",
            "telefone", telefone,
            "entregaId", entrega.getId()
        ));
    }

    private void enviarMensagemConfirmacao(String telefone, Long restauranteId,
            PedidoPublicoRequest request, List<ItemCarrinho> itens, Long pedidoId) {
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

            String msg = mensagemService.resolverTexto(restauranteId, "CHATBOT_PEDIDO_ENVIADO",
                Map.of("PEDIDO_ID", String.valueOf(pedidoId)));
            sb.append(msg);

            evolutionClient.enviarMensagem(telefone, sb.toString());
            mensagemWhatsappService.registrar(restauranteId, telefone, DirecaoMensagem.ENVIADA, sb.toString());
        } catch (Exception e) {
            log.warn("Erro ao enviar mensagem de confirmação do pedido público: {}", e.getMessage());
        }
    }

    private String normalizarTelefone(String telefone) {
        if (telefone.endsWith("@lid")) return telefone;
        String digits = telefone.replaceAll("[^0-9]", "");
        if (!digits.startsWith("55") && digits.length() <= 11) {
            digits = "55" + digits;
        }
        return digits;
    }
}
