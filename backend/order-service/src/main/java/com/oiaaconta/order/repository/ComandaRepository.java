package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.enums.StatusComanda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    Optional<Comanda> findByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);
    Optional<Comanda> findByIdAndRestauranteId(Long id, Long restauranteId);

    // Variantes usadas pelos call-sites que montam ComandaResponse fora de uma
    // transação própria (ComandaService.buscarComandaAtiva/buscarPorId/
    // listarAbertas/listarAguardandoPagamento, FinanceiroService) e por isso
    // precisam de pedidos + pedidos.itens (Pedido.itens agora é LAZY, era
    // EAGER) carregados explicitamente numa única query.
    @EntityGraph(attributePaths = "pedidos.itens")
    Optional<Comanda> findWithPedidosEItensByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);

    // Sem @EntityGraph aninhado aqui — "pedidos.itens" com uma List<Comanda>
    // como raiz dispara MultipleBagFetchException do Hibernate (2 bags/List
    // não podem ser fetch-joined ao mesmo tempo). "pedidos" já vem eager
    // (fetch abaixo); "itens" de cada pedido continua LAZY, mas carrega sem
    // erro graças ao open-session-in-view (a sessão fica aberta durante toda
    // a request).
    @EntityGraph(attributePaths = "pedidos")
    List<Comanda> findWithPedidosEItensByRestauranteIdAndStatus(Long restauranteId, StatusComanda status);

    @EntityGraph(attributePaths = "pedidos.itens")
    Optional<Comanda> findWithPedidosEItensByIdAndRestauranteId(Long id, Long restauranteId);

    @EntityGraph(attributePaths = "pedidos")
    List<Comanda> findWithPedidosEItensByRestauranteIdAndStatusAndClosedAtBetween(
        Long restauranteId, StatusComanda status,
        java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
}
