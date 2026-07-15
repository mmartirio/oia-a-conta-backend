package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.SessaoCaixa;
import com.oiaaconta.order.enums.StatusSessaoCaixa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessaoCaixaRepository extends JpaRepository<SessaoCaixa, Long> {
    Optional<SessaoCaixa> findFirstByRestauranteIdAndStatusOrderByAbertoEmDesc(Long restauranteId, StatusSessaoCaixa status);
    List<SessaoCaixa> findByRestauranteIdOrderByAbertoEmDesc(Long restauranteId);
}
