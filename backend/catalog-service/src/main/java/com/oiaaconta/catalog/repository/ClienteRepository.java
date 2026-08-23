package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByRestauranteIdOrderByNomeAsc(Long restauranteId);
    List<Cliente> findByRestauranteIdAndAtivoTrueOrderByNomeAsc(Long restauranteId);
    Optional<Cliente> findByIdAndRestauranteId(Long id, Long restauranteId);
    Optional<Cliente> findByRestauranteIdAndTelefone(Long restauranteId, String telefone);
    boolean existsByRestauranteIdAndTelefone(Long restauranteId, String telefone);
}
