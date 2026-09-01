package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.ComboGrupoProduto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboGrupoProdutoRepository extends JpaRepository<ComboGrupoProduto, Long> {
    List<ComboGrupoProduto> findByGrupoId(Long grupoId);
    List<ComboGrupoProduto> findByGrupoIdIn(List<Long> grupoIds);
}
