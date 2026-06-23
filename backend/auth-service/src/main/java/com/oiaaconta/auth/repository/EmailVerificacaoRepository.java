package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.EmailVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailVerificacaoRepository extends JpaRepository<EmailVerificacao, Long> {

    Optional<EmailVerificacao> findTopByEmailAndUsadoFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE EmailVerificacao e SET e.usado = true WHERE e.email = :email AND e.usado = false")
    void invalidarTodosPorEmail(String email);
}
