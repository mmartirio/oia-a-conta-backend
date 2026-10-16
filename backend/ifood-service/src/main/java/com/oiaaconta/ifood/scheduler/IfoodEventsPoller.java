package com.oiaaconta.ifood.scheduler;

import com.oiaaconta.ifood.client.IfoodOrderClient;
import com.oiaaconta.ifood.client.OrderClient;
import com.oiaaconta.ifood.dto.ifood.IfoodEventoDto;
import com.oiaaconta.ifood.dto.ifood.IfoodPedidoDto;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodVinculoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Recebimento de pedidos do iFood: como não há webhook configurado (modelo
// de polling, mais simples de operar), varre /events:polling periodicamente
// pra cada loja vinculada. Tipos de evento tratados: "PLC" (pedido colocado)
// e "CAN" (pedido cancelado do lado do iFood/cliente — ex: cliente desiste,
// ou o próprio iFood cancela por timeout). Os demais (confirmação, despacho
// etc.) só são reconhecidos/ack'd, porque nesses casos o fluxo é sempre
// disparado por nós (EntregaService -> IfoodClient), não o contrário.
@Component
@RequiredArgsConstructor
@Slf4j
public class IfoodEventsPoller {

    private static final String EVENTO_PEDIDO_COLOCADO = "PLC";
    private static final String EVENTO_PEDIDO_CANCELADO = "CAN";
    private static final String EVENTO_PEDIDO_CONCLUIDO = "CON";

    private final IfoodMerchantRepository merchantRepository;
    private final IfoodOrderClient ifoodOrderClient;
    private final OrderClient orderClient;
    private final IfoodVinculoService vinculoService;

    @Scheduled(fixedDelayString = "${ifood.events-poll-interval-ms:30000}")
    public void executar() {
        for (IfoodMerchant merchant : merchantRepository.findByAtivoTrue()) {
            try {
                processarMerchant(merchant);
            } catch (Exception e) {
                log.warn("Falha no polling de eventos iFood do restaurante {}: {}", merchant.getRestauranteId(), e.getMessage());
            }
        }
    }

    private void processarMerchant(IfoodMerchant merchant) {
        String token = "Bearer " + vinculoService.garantirTokenValido(merchant);
        List<IfoodEventoDto> eventos = ifoodOrderClient.polling(token);
        if (eventos == null || eventos.isEmpty()) return;

        for (IfoodEventoDto evento : eventos) {
            if (EVENTO_PEDIDO_COLOCADO.equals(evento.getCode())) {
                try {
                    processarPedidoColocado(merchant, token, evento.getOrderId());
                } catch (Exception e) {
                    log.error("Falha ao processar pedido iFood {} (restaurante {}): {}",
                        evento.getOrderId(), merchant.getRestauranteId(), e.getMessage(), e);
                }
            } else if (EVENTO_PEDIDO_CANCELADO.equals(evento.getCode())) {
                try {
                    orderClient.cancelarEntregaIfood(merchant.getRestauranteId(), evento.getOrderId());
                    log.info("iFood: pedido {} CANCELADO pelo lado do iFood (evento CAN recebido, entrega local atualizada)", evento.getOrderId());
                } catch (Exception e) {
                    log.error("Falha ao processar cancelamento do pedido iFood {} (restaurante {}): {}",
                        evento.getOrderId(), merchant.getRestauranteId(), e.getMessage(), e);
                }
            } else if (EVENTO_PEDIDO_CONCLUIDO.equals(evento.getCode())) {
                try {
                    orderClient.concluirEntregaIfood(merchant.getRestauranteId(), evento.getOrderId());
                    log.info("iFood: pedido {} CONCLUÍDO pelo lado do iFood (evento CON recebido, entrega local atualizada)", evento.getOrderId());
                } catch (Exception e) {
                    log.error("Falha ao processar conclusão do pedido iFood {} (restaurante {}): {}",
                        evento.getOrderId(), merchant.getRestauranteId(), e.getMessage(), e);
                }
            }

            // Um ack por evento, não em lote — um único id problemático (ou
            // uma falha transitória isolada) não pode derrubar a confirmação
            // dos demais eventos do mesmo ciclo. O que falhar aqui volta a
            // aparecer no próximo polling (iFood reenvia o que não foi
            // confirmado), então não precisa de retry manual.
            try {
                ifoodOrderClient.acknowledge(token, List.of(Map.of("id", evento.getId())));
            } catch (Exception e) {
                log.warn("Falha ao confirmar recebimento do evento iFood {} (restaurante {}): {}",
                    evento.getId(), merchant.getRestauranteId(), e.getMessage());
            }
        }
    }

    private void processarPedidoColocado(IfoodMerchant merchant, String token, String ifoodOrderId) {
        Long restauranteId = merchant.getRestauranteId();
        IfoodPedidoDto pedido = ifoodOrderClient.buscarPedido(token, ifoodOrderId);

        // Confirma ANTES de tentar montar a Entrega — o iFood cancela
        // sozinho se a loja não confirmar em poucos minutos, e isso não
        // pode ficar refém de um problema na criação da Entrega aqui (ex:
        // pedido de teste do Portal do Desenvolvedor com um item genérico
        // deles, sem correspondência nenhuma no nosso catálogo).
        ifoodOrderClient.confirmar(token, ifoodOrderId);
        log.info("iFood: pedido {} (#{}) CONFIRMADO (POST /orders/{}/confirm)", ifoodOrderId, pedido.getDisplayId(), ifoodOrderId);

        OrderClient.EntregaRequest request = new OrderClient.EntregaRequest();
        request.clienteNome = pedido.getCustomer() != null && pedido.getCustomer().getName() != null
            ? pedido.getCustomer().getName() : "Cliente iFood";
        request.clienteTelefone = pedido.getCustomer() != null && pedido.getCustomer().getPhone() != null
            ? pedido.getCustomer().getPhone().getNumber() : null;

        IfoodPedidoDto.Endereco endereco = pedido.getDelivery() != null ? pedido.getDelivery().getDeliveryAddress() : null;
        request.enderecoRua = endereco != null && endereco.getStreetName() != null ? endereco.getStreetName() : "Não informado";
        request.enderecoNumero = endereco != null && endereco.getStreetNumber() != null ? endereco.getStreetNumber() : "S/N";
        request.enderecoBairro = endereco != null ? endereco.getNeighborhood() : null;
        request.enderecoCidade = endereco != null ? endereco.getCity() : null;
        request.enderecoComplemento = endereco != null ? endereco.getComplement() : null;

        // O pagamento do pedido iFood é resolvido no ecossistema deles (app
        // do cliente ou dinheiro/maquininha na entrega, já contabilizado
        // pelo entregador) — não é um dos fluxos de cobrança do nosso caixa,
        // então usamos um método neutro só pra preencher o campo obrigatório.
        request.metodoPagamento = "DINHEIRO";
        request.observacao = "Pedido iFood #" + pedido.getDisplayId();
        request.origemIfood = true;
        request.ifoodOrderId = pedido.getId();
        request.ifoodDisplayId = pedido.getDisplayId();

        List<OrderClient.ItemEntregaRequest> itens = new ArrayList<>();
        List<IfoodPedidoDto.Item> itensPedido = pedido.getItems() != null ? pedido.getItems() : List.of();
        for (IfoodPedidoDto.Item item : itensPedido) {
            itens.add(resolverItem(item));
        }
        if (itens.isEmpty()) {
            log.warn("Pedido iFood {} confirmado, mas sem nenhum item no corpo do pedido", ifoodOrderId);
            return;
        }
        request.itens = itens;

        orderClient.criarEntrega(restauranteId, request);
    }

    // Sentinela pra item "avulso" — não corresponde a nenhum produto/combo
    // sincronizado por nós (ex: item genérico de pedido de teste do Portal
    // do Desenvolvedor, "PRODUTO 1/2 - NÃO ENTREGAR"). EntregaService não
    // valida produtoId contra o catálogo (só precisa ser não-nulo), então dá
    // pra usar esse valor sem afetar nenhuma regra de negócio — o nome real
    // do item (vindo do iFood) é preservado em produtoNome pro atendente ver
    // do que se trata na tela de pedidos.
    private static final Long PRODUTO_AVULSO_ID = -1L;

    // externalCode é o mesmo valor gravado na sincronização de catálogo
    // ("produto-123"/"combo-45", ver IfoodCatalogSyncService) — o iFood
    // ecoa esse código de volta no pedido, então dá pra resolver direto
    // pelo prefixo, sem round-trip no banco de mapeamentos.
    private OrderClient.ItemEntregaRequest resolverItem(IfoodPedidoDto.Item item) {
        String externalCode = item.getExternalCode();

        OrderClient.ItemEntregaRequest req = new OrderClient.ItemEntregaRequest();
        req.produtoNome = item.getName();
        req.quantidade = item.getQuantity() != null ? item.getQuantity() : 1;
        req.precoUnitario = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;

        if (externalCode != null && externalCode.startsWith("produto-")) {
            req.produtoId = Long.valueOf(externalCode.substring("produto-".length()));
            return req;
        }
        if (externalCode != null && externalCode.startsWith("combo-")) {
            req.comboId = Long.valueOf(externalCode.substring("combo-".length()));
            req.comboQuantidade = req.quantidade;
            return req;
        }
        req.produtoId = PRODUTO_AVULSO_ID;
        return req;
    }
}
