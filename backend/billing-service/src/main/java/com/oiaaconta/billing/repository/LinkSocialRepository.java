package com.oiaaconta.billing.repository;

import com.oiaaconta.billing.entity.LinkSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LinkSocialRepository extends JpaRepository<LinkSocial, Long> {
    List<LinkSocial> findByAtivoTrueOrderByTipoAsc();
}
