package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.Entrega;
import com.oiaaconta.order.enums.StatusEntrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    // itens agora é FetchType.LAZY (era EAGER). Os finders usados pelos
    // endpoints de listagem (sem transação própria no service) usam
    // @EntityGraph para continuar carregando "itens" em 1 query.
    @EntityGraph(attributePaths = "itens")
    Page<Entrega> findByRestauranteIdOrderByCreatedAtDesc(Long restauranteId, Pageable pageable);

    @EntityGraph(attributePaths = "itens")
    Page<Entrega> findByRestauranteIdAndStatusOrderByCreatedAtDesc(Long restauranteId, StatusEntrega status, Pageable pageable);

    Optional<Entrega> findByIdAndRestauranteId(Long id, Long restauranteId);

    // Ascendente por entregueAt — ordem de chegada na fila do caixa (mais
    // antiga primeiro), não por criação do pedido.
    @EntityGraph(attributePaths = "itens")
    Page<Entrega> findByRestauranteIdAndStatusAndPagamentoConfirmadoCaixaOrderByEntregueAtAsc(
        Long restauranteId, StatusEntrega status, Boolean pagamentoConfirmadoCaixa, Pageable pageable);

    @EntityGraph(attributePaths = "itens")
    List<Entrega> findByRestauranteIdAndPagamentoConfirmadoCaixaTrueAndEntregueAtBetween(
        Long restauranteId, java.time.LocalDateTime inicio, java.time.LocalDateTime fim);

    @EntityGraph(attributePaths = "itens")
    List<Entrega> findByRestauranteIdAndStatus(Long restauranteId, StatusEntrega status);

    // Entregas ativas de um entregador específico — usada pra sugestão de
    // rota (RotaService). Não precisa do EntityGraph de "itens": só usamos
    // endereço/coordenadas/status aqui, não os itens do pedido.
    List<Entrega> findByRestauranteIdAndEntregadorIdAndStatusIn(
        Long restauranteId, Long entregadorId, List<StatusEntrega> statuses);

    // Evita duplicar a Entrega se o mesmo evento de pedido do iFood for
    // reentregue no polling antes do acknowledgment ser confirmado.
    Optional<Entrega> findByRestauranteIdAndIfoodOrderId(Long restauranteId, String ifoodOrderId);
}
