package com.oiaaconta.order.service;

import com.oiaaconta.order.client.WhatsappClient;
import com.oiaaconta.order.dto.request.EntregaRequest;
import com.oiaaconta.order.dto.response.EntregaResponse;
import com.oiaaconta.order.dto.response.ItemEntregaResponse;
import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.entity.ItemEntrega;
import com.oiaaconta.order.enums.MetodoPagamento;
import com.oiaaconta.order.enums.StatusEntrega;
import com.oiaaconta.order.exception.BusinessException;
import com.oiaaconta.order.exception.ResourceNotFoundException;
import com.oiaaconta.order.repository.EntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final DeliveryOrchestrationService orchestrationService;
    private final WhatsappClient whatsappClient;
    private final ConfiguracaoService configuracaoService;

    @Transactional
    public EntregaResponse criar(Long restauranteId, EntregaRequest request) {
        Entrega entrega = Entrega.builder()
            .restauranteId(restauranteId)
            .clienteNome(request.getClienteNome())
            .clienteTelefone(request.getClienteTelefone())
            .enderecoRua(request.getEnderecoRua())
            .enderecoNumero(request.getEnderecoNumero())
            .enderecoBairro(request.getEnderecoBairro())
            .enderecoCidade(request.getEnderecoCidade())
            .enderecoComplemento(request.getEnderecoComplemento())
            .metodoPagamento(request.getMetodoPagamento())
            .parcelas(request.getParcelas())
            .observacao(request.getObservacao())
            .origemWhatsapp(request.isOrigemWhatsapp())
            .build();

        List<ItemEntrega> itens = request.getItens().stream()
            .map(item -> ItemEntrega.builder()
                .entrega(entrega)
                .produtoId(item.getProdutoId())
                .produtoNome(item.getProdutoNome())
                .quantidade(item.getQuantidade())
                .precoUnitario(item.getPrecoUnitario())
                .observacao(item.getObservacao())
                .build())
            .toList();
        entrega.setItens(itens);

        return toResponse(entregaRepository.save(entrega));
    }

    public List<EntregaResponse> listar(Long restauranteId) {
        return entregaRepository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public List<EntregaResponse> listarAguardando(Long restauranteId) {
        return entregaRepository.findByRestauranteIdAndStatusOrderByCreatedAtDesc(
            restauranteId, StatusEntrega.AGUARDANDO)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EntregaResponse aceitar(Long restauranteId, Long id, Long entregadorId, String entregadorNome) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.AGUARDANDO) {
            throw new BusinessException("Entrega não está aguardando");
        }
        entrega.setStatus(StatusEntrega.ACEITA);
        entrega.setEntregadorId(entregadorId);
        entrega.setEntregadorNome(entregadorNome);
        Entrega saved = entregaRepository.save(entrega);

        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            try {
                Long pedidoId = orchestrationService.criarPedidoCozinha(saved);
                saved.setPedidoCozinhaId(pedidoId);
                saved = entregaRepository.save(saved);
            } catch (Exception e) {
                log.warn("Falha ao criar pedido na cozinha para entrega #{}: {}", saved.getId(), e.getMessage());
            }
            notificarWhatsapp(restauranteId, saved.getId(), "PEDIDO_ACEITO");
        }

        return toResponse(saved);
    }

    @Transactional
    public EntregaResponse prontoParaEntrega(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ACEITA) {
            throw new BusinessException("Entrega não foi aceita ainda");
        }
        entrega.setStatus(StatusEntrega.PRONTO_PARA_ENTREGA);
        EntregaResponse resp = toResponse(entregaRepository.save(entrega));
        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_PRONTO");
        }
        return resp;
    }

    @Transactional
    public EntregaResponse saiu(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ACEITA && entrega.getStatus() != StatusEntrega.PRONTO_PARA_ENTREGA) {
            throw new BusinessException("Entrega não está pronta para sair");
        }
        entrega.setStatus(StatusEntrega.SAIU_PARA_ENTREGA);
        EntregaResponse resp = toResponse(entregaRepository.save(entrega));
        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_SAIU");
        }
        return resp;
    }

    @Transactional
    public EntregaResponse entregar(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.SAIU_PARA_ENTREGA) {
            throw new BusinessException("Entrega ainda não saiu");
        }
        entrega.setStatus(StatusEntrega.ENTREGUE);
        entrega.setEntregueAt(LocalDateTime.now());

        // PIX fica pendente no caixa; dinheiro e cartão são confirmados automaticamente
        if (entrega.getMetodoPagamento() != MetodoPagamento.PIX) {
            entrega.setPagamentoConfirmadoCaixa(true);
        }

        EntregaResponse resp = toResponse(entregaRepository.save(entrega));

        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            if (entrega.getMetodoPagamento() == MetodoPagamento.PIX) {
                String pixChave = configuracaoService.get(restauranteId).getPixChave();
                BigDecimal total = entrega.getItens().stream()
                    .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_PIX",
                    java.util.Map.of(
                        "PIX_CHAVE", pixChave != null ? pixChave : "",
                        "VALOR", String.format("%.2f", total)));
            } else {
                notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_ENTREGUE");
            }
        }

        return resp;
    }

    @Transactional
    public EntregaResponse cancelar(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() == StatusEntrega.ENTREGUE) {
            throw new BusinessException("Não é possível cancelar uma entrega já concluída");
        }
        entrega.setStatus(StatusEntrega.CANCELADA);
        return toResponse(entregaRepository.save(entrega));
    }

    @Transactional
    public EntregaResponse confirmarPagamento(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ENTREGUE) {
            throw new BusinessException("Pagamento só pode ser confirmado após a entrega");
        }
        if (Boolean.TRUE.equals(entrega.getPagamentoConfirmadoCaixa())) {
            throw new BusinessException("Pagamento já confirmado");
        }
        entrega.setPagamentoConfirmadoCaixa(true);
        return toResponse(entregaRepository.save(entrega));
    }

    public List<EntregaResponse> listarPendentesPagamento(Long restauranteId) {
        return entregaRepository
            .findByRestauranteIdAndStatusAndPagamentoConfirmadoCaixaOrderByCreatedAtDesc(
                restauranteId, StatusEntrega.ENTREGUE, false)
            .stream().map(this::toResponse).toList();
    }

    private void notificarWhatsapp(Long restauranteId, Long entregaId, String tipo) {
        notificarWhatsapp(restauranteId, entregaId, tipo, null);
    }

    private void notificarWhatsapp(Long restauranteId, Long entregaId, String tipo, java.util.Map<String, String> variaveis) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("entregaId", entregaId);
            payload.put("restauranteId", restauranteId);
            payload.put("tipo", tipo);
            if (variaveis != null) payload.put("variaveis", variaveis);
            whatsappClient.notificar(payload);
        } catch (Exception e) {
            log.warn("Falha ao notificar WhatsApp para entrega #{}: {}", entregaId, e.getMessage());
        }
    }

    private Entrega find(Long restauranteId, Long id) {
        return entregaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));
    }

    private EntregaResponse toResponse(Entrega e) {
        List<ItemEntregaResponse> itens = e.getItens().stream()
            .map(i -> ItemEntregaResponse.builder()
                .id(i.getId()).produtoId(i.getProdutoId())
                .produtoNome(i.getProdutoNome()).quantidade(i.getQuantidade())
                .precoUnitario(i.getPrecoUnitario()).observacao(i.getObservacao())
                .build())
            .toList();

        BigDecimal total = e.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EntregaResponse.builder()
            .id(e.getId()).restauranteId(e.getRestauranteId())
            .clienteNome(e.getClienteNome()).clienteTelefone(e.getClienteTelefone())
            .enderecoRua(e.getEnderecoRua()).enderecoNumero(e.getEnderecoNumero())
            .enderecoBairro(e.getEnderecoBairro()).enderecoCidade(e.getEnderecoCidade())
            .enderecoComplemento(e.getEnderecoComplemento())
            .entregadorId(e.getEntregadorId()).entregadorNome(e.getEntregadorNome())
            .status(e.getStatus().name())
            .metodoPagamento(e.getMetodoPagamento().name()).parcelas(e.getParcelas())
            .observacao(e.getObservacao()).total(total).itens(itens)
            .pedidoCozinhaId(e.getPedidoCozinhaId())
            .origemWhatsapp(Boolean.TRUE.equals(e.getOrigemWhatsapp()))
            .pagamentoConfirmadoCaixa(Boolean.TRUE.equals(e.getPagamentoConfirmadoCaixa()))
            .criadoEm(e.getCreatedAt()).entregueEm(e.getEntregueAt())
            .build();
    }
}
