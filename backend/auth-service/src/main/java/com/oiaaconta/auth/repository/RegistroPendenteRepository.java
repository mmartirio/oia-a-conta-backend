package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.RegistroPendente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RegistroPendenteRepository extends JpaRepository<RegistroPendente, Long> {

    Optional<RegistroPendente> findByEmail(String email);

    @Modifying
    @Query("DELETE FROM RegistroPendente r WHERE r.expiradoEm < :agora")
    void limparExpirados(LocalDateTime agora);
}
