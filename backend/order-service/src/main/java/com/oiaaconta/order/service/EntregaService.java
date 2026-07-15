package com.oiaaconta.order.service;

import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.client.WhatsappClient;
import com.oiaaconta.order.dto.NotificacaoLocalizacaoEntrega;
import com.oiaaconta.order.dto.NotificacaoMessage;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final NotificationClient notificationClient;

    @Transactional
    public EntregaResponse criar(Long restauranteId, EntregaRequest request) {
        if (request.isOrigemPdv() && request.getMetodoPagamento() == MetodoPagamento.PIX) {
            throw new BusinessException("Delivery criado pelo PDV não aceita PIX — o entregador recebe em dinheiro ou cartão e repassa ao caixa na volta.");
        }

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
            .origemPdv(request.isOrigemPdv())
            .build();

        // Pedido lançado por um funcionário (PDV/garçom) já é uma decisão da
        // casa — pula o gate de aceitar/rejeitar, que existe só para pedidos
        // que o próprio cliente fez sozinho (WhatsApp/cardápio).
        if (!request.isOrigemWhatsapp()) {
            entrega.setStatus(StatusEntrega.CONFIRMADA);
        }

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

        Entrega saved = entregaRepository.save(entrega);

        if (request.isOrigemWhatsapp()) {
            try {
                notificationClient.novaEntregaAguardando(NotificacaoMessage.builder()
                    .tipo("NOVA_ENTREGA_AGUARDANDO")
                    .pedidoId(saved.getId())
                    .restauranteId(restauranteId)
                    .mensagem("Novo pedido de " + saved.getClienteNome())
                    .build());
            } catch (Exception e) {
                log.warn("Falha ao notificar nova entrega aguardando #{}: {}", saved.getId(), e.getMessage());
            }
        } else {
            try {
                Long pedidoId = orchestrationService.criarPedidoCozinha(saved);
                saved.setPedidoCozinhaId(pedidoId);
                saved = entregaRepository.save(saved);
            } catch (Exception e) {
                log.warn("Falha ao criar pedido na cozinha para entrega #{}: {}", saved.getId(), e.getMessage());
            }
        }

        return toResponse(saved);
    }

    public Page<EntregaResponse> listar(Long restauranteId, Pageable pageable) {
        return entregaRepository.findByRestauranteIdOrderByCreatedAtDesc(restauranteId, pageable)
            .map(this::toResponse);
    }

    public Page<EntregaResponse> listarAguardando(Long restauranteId, Pageable pageable) {
        return entregaRepository.findByRestauranteIdAndStatusOrderByCreatedAtDesc(
            restauranteId, StatusEntrega.AGUARDANDO, pageable)
            .map(this::toResponse);
    }

    public Page<EntregaResponse> listarConfirmadas(Long restauranteId, Pageable pageable) {
        return entregaRepository.findByRestauranteIdAndStatusOrderByCreatedAtDesc(
            restauranteId, StatusEntrega.CONFIRMADA, pageable)
            .map(this::toResponse);
    }

    // Cozinha/admin decide se aceita o pedido do cliente (WhatsApp/cardápio).
    // Só a partir daqui a cozinha é notificada e o cliente recebe a confirmação.
    @Transactional
    public EntregaResponse confirmar(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.AGUARDANDO) {
            throw new BusinessException("Pedido não está aguardando confirmação");
        }
        entrega.setStatus(StatusEntrega.CONFIRMADA);
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

    // Cozinha/admin recusa o pedido do cliente — exige motivo, que é repassado ao cliente.
    @Transactional
    public EntregaResponse rejeitar(Long restauranteId, Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessException("Informe o motivo da recusa");
        }
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.AGUARDANDO) {
            throw new BusinessException("Pedido não está aguardando confirmação");
        }
        entrega.setStatus(StatusEntrega.CANCELADA);
        entrega.setMotivoRejeicao(motivo.trim());
        Entrega saved = entregaRepository.save(entrega);

        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, saved.getId(), "PEDIDO_REJEITADO",
                java.util.Map.of("MOTIVO", motivo.trim()));
        }

        return toResponse(saved);
    }

    @Transactional
    public EntregaResponse aceitar(Long restauranteId, Long id, Long entregadorId, String entregadorNome) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.CONFIRMADA) {
            throw new BusinessException("Entrega ainda não foi confirmada pela cozinha");
        }
        entrega.setStatus(StatusEntrega.ACEITA);
        entrega.setEntregadorId(entregadorId);
        entrega.setEntregadorNome(entregadorNome);
        Entrega saved = entregaRepository.save(entrega);
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
    @SuppressWarnings("null")
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

    public List<EntregaResponse> listarEmRota(Long restauranteId) {
        return entregaRepository.findByRestauranteIdAndStatus(restauranteId, StatusEntrega.SAIU_PARA_ENTREGA)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // Atualiza a posição GPS do entregador enquanto a entrega está a caminho —
    // usada pelo mapa de rastreamento ao vivo no dashboard do admin. Só o
    // entregador responsável pela entrega (ou admin) pode atualizar, e só
    // enquanto ela estiver de fato "a caminho".
    @Transactional
    public void atualizarLocalizacao(Long restauranteId, Long id, Long usuarioId, boolean isAdmin, Double latitude, Double longitude) {
        Entrega entrega = find(restauranteId, id);
        if (!isAdmin && !entrega.getEntregadorId().equals(usuarioId)) {
            throw new BusinessException("Você não é o entregador responsável por esta entrega");
        }
        if (entrega.getStatus() != StatusEntrega.SAIU_PARA_ENTREGA) {
            throw new BusinessException("Entrega não está a caminho");
        }
        entrega.setLatitude(latitude);
        entrega.setLongitude(longitude);
        entrega.setLocalizacaoAtualizadaEm(LocalDateTime.now());
        entregaRepository.save(entrega);

        try {
            notificationClient.localizacaoEntrega(NotificacaoLocalizacaoEntrega.builder()
                .restauranteId(restauranteId)
                .entregaId(id)
                .entregadorNome(entrega.getEntregadorNome())
                .latitude(latitude)
                .longitude(longitude)
                .atualizadoEm(entrega.getLocalizacaoAtualizadaEm().toString())
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar localização da entrega #{}: {}", id, e.getMessage());
        }
    }

    public Page<EntregaResponse> listarPendentesPagamento(Long restauranteId, Pageable pageable) {
        return entregaRepository
            .findByRestauranteIdAndStatusAndPagamentoConfirmadoCaixaOrderByCreatedAtDesc(
                restauranteId, StatusEntrega.ENTREGUE, false, pageable)
            .map(this::toResponse);
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

    @SuppressWarnings("null")
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
            .observacao(e.getObservacao()).motivoRejeicao(e.getMotivoRejeicao()).total(total).itens(itens)
            .pedidoCozinhaId(e.getPedidoCozinhaId())
            .origemWhatsapp(Boolean.TRUE.equals(e.getOrigemWhatsapp()))
            .origemPdv(Boolean.TRUE.equals(e.getOrigemPdv()))
            .pagamentoConfirmadoCaixa(Boolean.TRUE.equals(e.getPagamentoConfirmadoCaixa()))
            .criadoEm(e.getCreatedAt()).entregueEm(e.getEntregueAt())
            .latitude(e.getLatitude()).longitude(e.getLongitude())
            .localizacaoAtualizadaEm(e.getLocalizacaoAtualizadaEm())
            .build();
    }
}
