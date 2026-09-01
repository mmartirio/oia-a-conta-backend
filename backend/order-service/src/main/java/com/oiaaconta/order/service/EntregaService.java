package com.oiaaconta.order.service;

import com.oiaaconta.order.client.CatalogClient;
import com.oiaaconta.order.client.IfoodClient;
import com.oiaaconta.order.client.NotificationClient;
import com.oiaaconta.order.client.WhatsappClient;
import com.oiaaconta.order.dto.NotificacaoLocalizacaoEntrega;
import com.oiaaconta.order.dto.NotificacaoMessage;
import com.oiaaconta.order.dto.catalog.ComboResponseDto;
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
import java.util.ArrayList;
import java.util.Optional;
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
    private final CatalogClient catalogClient;
    private final FreteService freteService;
    private final IfoodClient ifoodClient;
    private final GeocodingService geocodingService;
    private final ComboEscolhaResolver comboEscolhaResolver;

    @Transactional
    public EntregaResponse criar(Long restauranteId, EntregaRequest request) {
        if (request.isOrigemPdv() && request.getMetodoPagamento() == MetodoPagamento.PIX) {
            throw new BusinessException("Delivery criado pelo PDV não aceita PIX — o entregador recebe em dinheiro ou cartão e repassa ao caixa na volta.");
        }
        // Sem maquininha na entrega quando quem leva é um entregador externo
        // (99/Uber Entrega etc.) — mesma trava do frontend (CardapioPublico),
        // repetida aqui pro checkout público (sem auth) não poder ser
        // contornado enviando CARTAO_CREDITO direto pela API.
        if (request.isOrigemWhatsapp() && request.getMetodoPagamento() == MetodoPagamento.CARTAO_CREDITO
                && configuracaoService.get(restauranteId).isEntregadorExterno()) {
            throw new BusinessException("Cartão de crédito não disponível — este restaurante usa entregador externo.");
        }
        if (request.isOrigemIfood()) {
            if (request.getIfoodOrderId() == null || request.getIfoodOrderId().isBlank()) {
                throw new BusinessException("Pedido do iFood sem ifoodOrderId");
            }
            // Idempotência: se o mesmo evento do iFood for reprocessado (ex:
            // o acknowledgment falhou depois de já termos criado a entrega),
            // devolve a entrega existente em vez de duplicar o pedido.
            Optional<Entrega> existente = entregaRepository.findByRestauranteIdAndIfoodOrderId(restauranteId, request.getIfoodOrderId());
            if (existente.isPresent()) {
                return toResponse(existente.get());
            }
        }

        // Pedido de PDV já chega geocodificado no cliente (ver
        // PdvNovoDelivery.tsx); WhatsApp/cardápio público não geocodifica no
        // checkout, então o endereço chega só como texto — sem coordenada
        // aqui, tanto o frete por distância quanto a sugestão de rota do
        // entregador ficam indisponíveis pro pedido (ver GeocodingService).
        Double enderecoLatitude = request.getEnderecoLatitude();
        Double enderecoLongitude = request.getEnderecoLongitude();
        if (enderecoLatitude == null || enderecoLongitude == null) {
            GeocodingService.Coordenada coordenada = geocodingService.geocodificar(enderecoParaGeocodificacao(request));
            if (coordenada != null) {
                enderecoLatitude = coordenada.lat();
                enderecoLongitude = coordenada.lng();
            }
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
            .enderecoLatitude(enderecoLatitude)
            .enderecoLongitude(enderecoLongitude)
            .metodoPagamento(request.getMetodoPagamento())
            .parcelas(request.getParcelas())
            .observacao(request.getObservacao())
            .origemWhatsapp(request.isOrigemWhatsapp())
            .origemPdv(request.isOrigemPdv())
            .origemIfood(request.isOrigemIfood())
            .ifoodOrderId(request.getIfoodOrderId())
            .build();

        // Pedido lançado por um funcionário (PDV/garçom) já é uma decisão da
        // casa — pula o gate de aceitar/rejeitar, que existe só para pedidos
        // que o próprio cliente fez sozinho (WhatsApp/cardápio/iFood). Vai
        // direto pra produção (ACEITA) — o entregador é capturado só depois,
        // no clique de "Saiu" (ver EntregaService.saiu).
        if (!request.isOrigemWhatsapp() && !request.isOrigemIfood()) {
            entrega.setStatus(StatusEntrega.ACEITA);
        }

        // ArrayList mutável, não .toList() (imutável) — "entrega" é salva de
        // novo mais abaixo (pra gravar pedidoCozinhaId) dentro da mesma
        // transação, e o merge dessa segunda save tenta fazer clear() na
        // coleção "itens"; numa lista imutável isso derruba com
        // UnsupportedOperationException.
        List<ItemEntrega> itens = new ArrayList<>();
        for (EntregaRequest.ItemRequest item : request.getItens()) {
            if (item.getComboId() != null) {
                itens.addAll(expandirCombo(entrega, item, restauranteId));
            } else if (item.getProdutoId() != null) {
                itens.add(ItemEntrega.builder()
                    .entrega(entrega)
                    .produtoId(item.getProdutoId())
                    .produtoNome(item.getProdutoNome())
                    .quantidade(item.getQuantidade())
                    .precoUnitario(item.getPrecoUnitario())
                    .observacao(item.getObservacao())
                    .build());
            } else {
                throw new BusinessException("Cada item precisa ter um produto ou um combo");
            }
        }
        entrega.setItens(itens);

        FreteService.FreteCalculado frete = freteService.calcular(
            restauranteId, enderecoLatitude, enderecoLongitude);
        if (frete.disponivel()) {
            entrega.setDistanciaKm(frete.distanciaKm());
            entrega.setValorFrete(frete.valorFrete());
        }

        Entrega saved = entregaRepository.save(entrega);

        if (request.isOrigemWhatsapp() || request.isOrigemIfood()) {
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
                log.warn("Falha ao criar pedido na cozinha para entrega #{}", saved.getId(), e);
            }
        }

        return toResponse(saved);
    }

    // Monta a string de endereço pra geocodificação — o checkout do WhatsApp/
    // cardápio público hoje só manda "enderecoRua" como texto livre (sem
    // número/bairro/cidade separados), então usa o que tiver disponível.
    // ", Brasil" no fim ajuda o Nominatim a desambiguar nomes de rua comuns
    // entre cidades diferentes.
    private String enderecoParaGeocodificacao(EntregaRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getEnderecoRua() != null && !request.getEnderecoRua().isBlank()) {
            sb.append(request.getEnderecoRua());
        }
        appendSeNaoVazio(sb, request.getEnderecoNumero());
        appendSeNaoVazio(sb, request.getEnderecoBairro());
        appendSeNaoVazio(sb, request.getEnderecoCidade());
        if (sb.isEmpty()) return null;
        sb.append(", Brasil");
        return sb.toString();
    }

    private void appendSeNaoVazio(StringBuilder sb, String valor) {
        if (valor != null && !valor.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(valor);
        }
    }

    // Busca a composição do combo (endpoint público — este fluxo não tem JWT
    // de usuário, é serviço-a-serviço a partir de um cliente anônimo) e
    // expande nos ItemEntrega reais dos sabores escolhidos pelo cliente (ver
    // ComboEscolhaResolver) — mesma lógica usada por PedidoService.
    private List<ItemEntrega> expandirCombo(Entrega entrega, EntregaRequest.ItemRequest item, Long restauranteId) {
        ComboResponseDto combo;
        try {
            combo = catalogClient.buscarComboPublico(restauranteId, item.getComboId());
        } catch (Exception e) {
            throw new BusinessException("Combo não encontrado");
        }
        if (!combo.isAtivo()) {
            throw new BusinessException("Combo '" + combo.getNome() + "' está inativo");
        }
        int comboQuantidade = item.getComboQuantidade() != null ? item.getComboQuantidade() : 1;
        if (comboQuantidade < 1) {
            throw new BusinessException("Quantidade de combo inválida");
        }

        List<ItemEntrega> expandido = new ArrayList<>();
        for (ComboEscolhaResolver.ItemResolvido resolvido : comboEscolhaResolver.resolver(combo, item.getSaboresEscolhidos(), comboQuantidade)) {
            expandido.add(ItemEntrega.builder()
                .entrega(entrega)
                .produtoId(resolvido.produtoId())
                .produtoNome(resolvido.produtoNome())
                .quantidade(resolvido.quantidade())
                .observacao(item.getObservacao())
                .precoUnitario(resolvido.precoUnitario())
                .comboId(combo.getId())
                .comboNome(combo.getNome())
                .build());
        }
        return expandido;
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
    // Vai direto pra produção (ACEITA) — sem estágio intermediário de "novo
    // pedido" esperando um entregador; o entregador é capturado só no "Saiu".
    @Transactional
    public EntregaResponse confirmar(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.AGUARDANDO) {
            throw new BusinessException("Pedido não está aguardando confirmação");
        }
        entrega.setStatus(StatusEntrega.ACEITA);
        Entrega saved = entregaRepository.save(entrega);

        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp()) || Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            try {
                Long pedidoId = orchestrationService.criarPedidoCozinha(saved);
                saved.setPedidoCozinhaId(pedidoId);
                saved = entregaRepository.save(saved);
            } catch (Exception e) {
                log.warn("Falha ao criar pedido na cozinha para entrega #{}", saved.getId(), e);
            }
        }
        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, saved.getId(), "PEDIDO_ACEITO");
        }
        if (Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            notificarIfood(saved, "CONFIRMADA");
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
        if (Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            notificarIfood(saved, "CANCELADA");
        }

        return toResponse(saved);
    }

    // Sem estágio manual de "aceitar" no fluxo principal (entrega já nasce em
    // ACEITA/produção), mas o painel do entregador ainda precisa de um jeito
    // de reservar uma entrega sem entregador definido — pra vários
    // entregadores não disputarem a mesma corrida sem saber. Não muda status.
    @Transactional
    public EntregaResponse atribuirEntregador(Long restauranteId, Long id, Long entregadorId, String entregadorNome) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ACEITA && entrega.getStatus() != StatusEntrega.PRONTO_PARA_ENTREGA) {
            throw new BusinessException("Entrega não está disponível para ser assumida");
        }
        if (entrega.getEntregadorId() != null) {
            throw new BusinessException("Esta entrega já foi assumida por outro entregador");
        }
        entrega.setEntregadorId(entregadorId);
        entrega.setEntregadorNome(entregadorNome);
        return toResponse(entregaRepository.save(entrega));
    }

    @Transactional
    public EntregaResponse prontoParaEntrega(Long restauranteId, Long id) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ACEITA) {
            throw new BusinessException("Entrega não foi aceita ainda");
        }
        entrega.setStatus(StatusEntrega.PRONTO_PARA_ENTREGA);
        EntregaResponse resp = toResponse(entregaRepository.save(entrega));
        // Espelha pro board da cozinha — clicar "Pronto" no Delivery também
        // fecha o card lá (mesmo efeito do cozinheiro clicar "Marcar Pronto").
        try {
            orchestrationService.propagarProntoParaCozinha(entrega);
        } catch (Exception e) {
            log.warn("Falha ao propagar PRONTO pra cozinha (entrega #{}): {}", entrega.getId(), e.getMessage());
        }
        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_PRONTO");
        }
        if (Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            notificarIfood(entrega, "PRONTO_PARA_ENTREGA");
        }
        return resp;
    }

    @Transactional
    public EntregaResponse saiu(Long restauranteId, Long id, Long userId, String userNome) {
        Entrega entrega = find(restauranteId, id);
        if (entrega.getStatus() != StatusEntrega.ACEITA && entrega.getStatus() != StatusEntrega.PRONTO_PARA_ENTREGA) {
            throw new BusinessException("Entrega não está pronta para sair");
        }
        entrega.setStatus(StatusEntrega.SAIU_PARA_ENTREGA);
        // Sem "aceitar" manual, o entregador é quem quer que clique "Saiu"
        // primeiro — captura no pickup, não antes.
        if (entrega.getEntregadorId() == null) {
            entrega.setEntregadorId(userId);
            entrega.setEntregadorNome(userNome);
        }
        EntregaResponse resp = toResponse(entregaRepository.save(entrega));
        // Entregador pegou o pedido — sai da tela da cozinha.
        try {
            orchestrationService.finalizarPedidoCozinha(entrega);
        } catch (Exception e) {
            log.warn("Falha ao finalizar pedido na cozinha (entrega #{}): {}", entrega.getId(), e.getMessage());
        }
        if (Boolean.TRUE.equals(entrega.getOrigemWhatsapp())) {
            notificarWhatsapp(restauranteId, entrega.getId(), "PEDIDO_SAIU");
        }
        if (Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            notificarIfood(entrega, "SAIU_PARA_ENTREGA");
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
        if (Boolean.TRUE.equals(entrega.getOrigemIfood())) {
            notificarIfood(entrega, "ENTREGUE");
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
            .findByRestauranteIdAndStatusAndPagamentoConfirmadoCaixaOrderByEntregueAtAsc(
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

    // Espelha notificarWhatsapp — efeito colateral best-effort, nunca deve
    // travar a mudança de status local por uma falha na integração externa.
    // "status" aqui usa os mesmos nomes do StatusEntrega (CONFIRMADA,
    // PRONTO_PARA_ENTREGA, SAIU_PARA_ENTREGA, ENTREGUE, CANCELADA) —
    // ifood-service traduz pra chamada certa da Order API do iFood.
    private void notificarIfood(Entrega entrega, String status) {
        try {
            ifoodClient.atualizarStatusPedido(java.util.Map.of(
                "restauranteId", entrega.getRestauranteId(),
                "ifoodOrderId", entrega.getIfoodOrderId(),
                "status", status));
        } catch (Exception e) {
            log.warn("Falha ao notificar iFood (pedido {}) status {}: {}", entrega.getIfoodOrderId(), status, e.getMessage());
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
                .comboId(i.getComboId()).comboNome(i.getComboNome())
                .build())
            .toList();

        BigDecimal subtotalItens = e.getItens().stream()
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorFrete = e.getValorFrete() != null ? e.getValorFrete() : BigDecimal.ZERO;
        BigDecimal total = subtotalItens.add(valorFrete);

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
            .enderecoLatitude(e.getEnderecoLatitude()).enderecoLongitude(e.getEnderecoLongitude())
            .distanciaKm(e.getDistanciaKm()).valorFrete(valorFrete)
            .origemIfood(Boolean.TRUE.equals(e.getOrigemIfood())).ifoodOrderId(e.getIfoodOrderId())
            .build();
    }

    // Preview do frete antes de confirmar o pedido (não persiste nada) — o
    // PDV/garçom usa isso pra mostrar o valor do frete pro cliente antes de
    // finalizar a venda.
    public FreteService.FreteCalculado calcularFretePreview(Long restauranteId, Double enderecoLatitude, Double enderecoLongitude) {
        return freteService.calcular(restauranteId, enderecoLatitude, enderecoLongitude);
    }
}
