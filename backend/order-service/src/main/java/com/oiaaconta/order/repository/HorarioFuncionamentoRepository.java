package com.oiaaconta.order.repository;

import com.oiaaconta.order.entity.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Long> {

    List<HorarioFuncionamento> findByRestauranteIdOrderByDiaSemanaAscHoraAberturaAsc(Long restauranteId);

    void deleteByRestauranteId(Long restauranteId);
}
