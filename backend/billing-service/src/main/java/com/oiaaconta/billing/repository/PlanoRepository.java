package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoRepository extends JpaRepository<Plano, Long> {
    List<Plano> findByAtivoTrueOrderByPrecoMensalAsc();
}
