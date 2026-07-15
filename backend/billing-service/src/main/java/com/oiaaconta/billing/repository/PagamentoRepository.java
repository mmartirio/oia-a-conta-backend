package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.Pagamento;
import com.oiaaconta.billing.enums.StatusPagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    Page<Pagamento> findByContratoIdOrderByCreatedAtDesc(Long contratoId, Pageable pageable);

    Optional<Pagamento> findByMpPaymentId(String mpPaymentId);

    @Query("SELECT COALESCE(SUM(p.valor), 0) FROM Pagamento p WHERE p.status = 'PAGO' AND p.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal somarReceitaPeriodo(LocalDate inicio, LocalDate fim);

    @Query("SELECT p FROM Pagamento p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Pagamento> findByStatus(StatusPagamento status);
}
