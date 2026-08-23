package com.oiaaconta.ifood.repository;

import com.oiaaconta.ifood.entity.IfoodMerchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IfoodMerchantRepository extends JpaRepository<IfoodMerchant, Long> {
    Optional<IfoodMerchant> findByRestauranteId(Long restauranteId);
    List<IfoodMerchant> findByAtivoTrue();
}
