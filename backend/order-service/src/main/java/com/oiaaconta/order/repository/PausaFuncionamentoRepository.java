package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.PausaFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PausaFuncionamentoRepository extends JpaRepository<PausaFuncionamento, Long> {

    List<PausaFuncionamento> findByRestauranteIdAndFimAfterOrderByInicioAsc(Long restauranteId, LocalDateTime agora);

    Optional<PausaFuncionamento> findByIdAndRestauranteId(Long id, Long restauranteId);
}
