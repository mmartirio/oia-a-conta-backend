package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Comanda;
import com.oiaaconta.order.enums.StatusComanda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {
    Optional<Comanda> findByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);
    Optional<Comanda> findByIdAndRestauranteId(Long id, Long restauranteId);

    // Variantes usadas pelos call-sites que montam ComandaResponse fora de uma
    // transação própria (ComandaService.buscarComandaAtiva/buscarPorId/
    // listarAbertas/listarAguardandoPagamento, FinanceiroService) e por isso
    // precisam de "pedidos" carregado numa única query (Pedido.itens agora é
    // LAZY, era EAGER).
    //
    // Nenhuma delas usa "pedidos.itens" no @EntityGraph — fetch-join de
    // "pedidos" (bag) + "itens" (outro bag, aninhado) dispara
    // MultipleBagFetchException do Hibernate independente da raiz ser
    // Optional<Comanda> ou List<Comanda> (2 bags não podem ser fetch-joined
    // simultaneamente). "pedidos" vem eager (fetch abaixo); "itens" de cada
    // pedido continua LAZY, mas carrega sem erro graças ao
    // open-session-in-view (a sessão fica aberta durante toda a request).
    @EntityGraph(attributePaths = "pedidos")
    Optional<Comanda> findWithPedidosEItensByMesaIdAndStatusAndRestauranteId(Long mesaId, StatusComanda status, Long restauranteId);

    @EntityGraph(attributePaths = "pedidos")
    List<Comanda> findWithPedidosEItensByRestauranteIdAndStatus(Long restauranteId, StatusComanda status);

    @EntityGraph(attributePaths = "pedidos")
    Optional<Comanda> findWithPedidosEItensByIdAndRestauranteId(Long id, Long restauranteId);

    @EntityGraph(attributePaths = "pedidos")
    List<Comanda> findWithPedidosEItensByRestauranteIdAndStatusAndClosedAtBetween(
        Long restauranteId, StatusComanda status,
        java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    // Gasto histórico acumulado do cliente (só comandas já fechadas/pagas) —
    // usado como input pro catalog-service avaliar o requisito de promoções
    // (catalog não tem acesso a dados de venda, então esse cálculo é feito aqui).
    @Query("SELECT COALESCE(SUM(c.valorTotal), 0) FROM Comanda c " +
        "WHERE c.restauranteId = :restauranteId AND c.clienteId = :clienteId AND c.status = 'FECHADA'")
    BigDecimal gastoHistoricoCliente(@Param("restauranteId") Long restauranteId, @Param("clienteId") Long clienteId);
}
