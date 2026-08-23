package com.oiaaconta.ifood.repository;

import com.oiaaconta.ifood.entity.IfoodMapeamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IfoodMapeamentoRepository extends JpaRepository<IfoodMapeamento, Long> {
    Optional<IfoodMapeamento> findByRestauranteIdAndTipoAndLocalId(Long restauranteId, String tipo, Long localId);
    Optional<IfoodMapeamento> findByRestauranteIdAndTipoAndIfoodId(Long restauranteId, String tipo, String ifoodId);
    List<IfoodMapeamento> findByRestauranteIdAndTipo(Long restauranteId, String tipo);
}
