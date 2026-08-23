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
// pra cada loja vinculada. Único tipo de evento tratado por enquanto: "PLC"
// (pedido colocado) — os demais (confirmação, despacho etc. originados do
// lado do iFood) só são reconhecidos, não processados, porque o fluxo
// nesses casos é sempre disparado por nós (EntregaService -> IfoodClient),
// não o contrário.
@Component
@RequiredArgsConstructor
@Slf4j
public class IfoodEventsPoller {

    private static final String EVENTO_PEDIDO_COLOCADO = "PLC";

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

        List<Map<String, String>> paraConfirmar = new ArrayList<>();
        for (IfoodEventoDto evento : eventos) {
            if (EVENTO_PEDIDO_COLOCADO.equals(evento.getCode())) {
                try {
                    processarPedidoColocado(merchant, token, evento.getOrderId());
                } catch (Exception e) {
                    log.error("Falha ao processar pedido iFood {} (restaurante {}): {}",
                        evento.getOrderId(), merchant.getRestauranteId(), e.getMessage(), e);
                }
            }
            paraConfirmar.add(Map.of("id", evento.getId()));
        }

        try {
            ifoodOrderClient.acknowledge(token, paraConfirmar);
        } catch (Exception e) {
            log.warn("Falha ao confirmar recebimento de eventos iFood (restaurante {}): {}", merchant.getRestauranteId(), e.getMessage());
        }
    }

    private void processarPedidoColocado(IfoodMerchant merchant, String token, String ifoodOrderId) {
        Long restauranteId = merchant.getRestauranteId();
        IfoodPedidoDto pedido = ifoodOrderClient.buscarPedido(token, ifoodOrderId);

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

        List<OrderClient.ItemEntregaRequest> itens = new ArrayList<>();
        List<IfoodPedidoDto.Item> itensPedido = pedido.getItems() != null ? pedido.getItems() : List.of();
        for (IfoodPedidoDto.Item item : itensPedido) {
            OrderClient.ItemEntregaRequest itemRequest = resolverItem(item);
            if (itemRequest == null) {
                throw new IllegalStateException("Item '" + item.getName() + "' (externalCode=" + item.getExternalCode()
                    + ") sem mapeamento local — sincronize o catálogo antes de receber pedidos");
            }
            itens.add(itemRequest);
        }
        if (itens.isEmpty()) {
            throw new IllegalStateException("Pedido iFood " + ifoodOrderId + " sem itens resolvíveis");
        }
        request.itens = itens;

        orderClient.criarEntrega(restauranteId, request);

        // Confirma o pedido imediatamente — o iFood cancela sozinho se a
        // loja não confirmar em poucos minutos.
        ifoodOrderClient.confirmar(token, ifoodOrderId);
    }

    // externalCode é o mesmo valor gravado na sincronização de catálogo
    // ("produto-123"/"combo-45", ver IfoodCatalogSyncService) — o iFood
    // ecoa esse código de volta no pedido, então dá pra resolver direto
    // pelo prefixo, sem round-trip no banco de mapeamentos.
    private OrderClient.ItemEntregaRequest resolverItem(IfoodPedidoDto.Item item) {
        String externalCode = item.getExternalCode();
        if (externalCode == null) return null;

        OrderClient.ItemEntregaRequest req = new OrderClient.ItemEntregaRequest();
        req.produtoNome = item.getName();
        req.quantidade = item.getQuantity() != null ? item.getQuantity() : 1;
        req.precoUnitario = item.getUnitPrice() != null && item.getUnitPrice().getValue() != null
            ? item.getUnitPrice().getValue() : BigDecimal.ZERO;

        if (externalCode.startsWith("produto-")) {
            req.produtoId = Long.valueOf(externalCode.substring("produto-".length()));
            return req;
        }
        if (externalCode.startsWith("combo-")) {
            req.comboId = Long.valueOf(externalCode.substring("combo-".length()));
            req.comboQuantidade = req.quantidade;
            return req;
        }
        return null;
    }
}
