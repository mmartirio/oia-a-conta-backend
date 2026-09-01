package com.oiaaconta.catalog.repository;

import com.oiaaconta.catalog.entity.ComboGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboGrupoRepository extends JpaRepository<ComboGrupo, Long> {
    List<ComboGrupo> findByComboIdOrderByOrdemAsc(Long comboId);
    void deleteByComboId(Long comboId);
}
