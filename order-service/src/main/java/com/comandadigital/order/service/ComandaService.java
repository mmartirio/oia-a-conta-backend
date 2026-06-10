package com.comandadigital.order.service;

import com.comandadigital.order.client.TableClient;
import com.comandadigital.order.dto.request.FecharComandaRequest;
import com.comandadigital.order.dto.response.ComandaResponse;
import com.comandadigital.order.dto.response.ItemPedidoResponse;
import com.comandadigital.order.dto.response.PedidoResponse;
import com.comandadigital.order.entity.Comanda;
import com.comandadigital.order.entity.Pedido;
import com.comandadigital.order.enums.StatusComanda;
import com.comandadigital.order.exception.BusinessException;
import com.comandadigital.order.exception.ResourceNotFoundException;
import com.comandadigital.order.repository.ComandaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComandaService {

    private final ComandaRepository comandaRepository;
    private final TableClient tableClient;

    @Transactional
    public ComandaResponse abrirComanda(Long restauranteId, Long mesaId, Integer mesaNumero,
                                         Long garconId, String garconNome, String authHeader) {
        comandaRepository.findByMesaIdAndStatusAndRestauranteId(mesaId, StatusComanda.ABERTA, restauranteId)
            .ifPresent(c -> { throw new BusinessException("Mesa já possui comanda aberta"); });

        Comanda comanda = comandaRepository.save(Comanda.builder()
            .restauranteId(restauranteId)
            .mesaId(mesaId)
            .mesaNumero(mesaNumero)
            .garconId(garconId)
            .garconNome(garconNome)
            .status(StatusComanda.ABERTA)
            .build());

        try {
            tableClient.atualizarStatus(authHeader, restauranteId, mesaId, Map.of("status", "OCUPADA"));
        } catch (Exception e) {
            log.warn("Falha ao atualizar status da mesa via Feign: {}", e.getMessage());
        }

        return toResponse(comanda);
    }

    public ComandaResponse buscarComandaAtiva(Long restauranteId, Long mesaId) {
        return toResponse(
            comandaRepository.findByMesaIdAndStatusAndRestauranteId(mesaId, StatusComanda.ABERTA, restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma comanda aberta para esta mesa")));
    }

    public ComandaResponse buscarPorId(Long restauranteId, Long id) {
        return toResponse(
            comandaRepository.findByIdAndRestauranteId(id, restauranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada")));
    }

    public List<ComandaResponse> listarAbertas(Long restauranteId) {
        return comandaRepository.findByRestauranteIdAndStatus(restauranteId, StatusComanda.ABERTA)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ComandaResponse fecharComanda(Long restauranteId, Long id,
                                          FecharComandaRequest request, String authHeader) {
        Comanda comanda = comandaRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada"));
        if (comanda.getStatus() != StatusComanda.ABERTA) {
            throw new BusinessException("Comanda já está fechada");
        }

        comanda.setStatus(StatusComanda.FECHADA);
        comanda.setMetodoPagamento(request.getMetodoPagamento());
        comanda.setClosedAt(LocalDateTime.now());
        Comanda saved = comandaRepository.save(comanda);

        try {
            tableClient.atualizarStatus(authHeader, restauranteId, comanda.getMesaId(),
                Map.of("status", "DISPONIVEL"));
        } catch (Exception e) {
            log.warn("Falha ao liberar mesa via Feign: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    ComandaResponse toResponse(Comanda c) {
        List<PedidoResponse> pedidosResp = c.getPedidos().stream()
            .map(this::pedidoToResponse).toList();

        BigDecimal total = c.getPedidos().stream()
            .flatMap(p -> p.getItens().stream())
            .map(i -> i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ComandaResponse.builder()
            .id(c.getId()).restauranteId(c.getRestauranteId())
            .mesaId(c.getMesaId()).mesaNumero(c.getMesaNumero())
            .garconId(c.getGarconId()).garconNome(c.getGarconNome())
            .status(c.getStatus().name())
            .metodoPagamento(c.getMetodoPagamento() != null ? c.getMetodoPagamento().name() : null)
            .total(total).pedidos(pedidosResp)
            .createdAt(c.getCreatedAt()).closedAt(c.getClosedAt())
            .build();
    }

    private PedidoResponse pedidoToResponse(Pedido p) {
        List<ItemPedidoResponse> itens = p.getItens().stream()
            .map(i -> ItemPedidoResponse.builder()
                .id(i.getId()).produtoId(i.getProdutoId())
                .produtoNome(i.getProdutoNome()).quantidade(i.getQuantidade())
                .observacao(i.getObservacao()).precoUnitario(i.getPrecoUnitario())
                .subtotal(i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())))
                .build())
            .toList();
        return PedidoResponse.builder()
            .id(p.getId()).comandaId(p.getComanda().getId())
            .restauranteId(p.getRestauranteId())
            .mesaNumero(p.getComanda().getMesaNumero())
            .garconId(p.getComanda().getGarconId())
            .garconNome(p.getComanda().getGarconNome())
            .status(p.getStatus().name()).observacao(p.getObservacao())
            .itens(itens).createdAt(p.getCreatedAt()).readyAt(p.getReadyAt())
            .build();
    }
}
