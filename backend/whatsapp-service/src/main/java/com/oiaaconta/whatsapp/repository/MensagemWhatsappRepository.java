package com.oiaaconta.whatsapp.repository;

import com.oiaaconta.whatsapp.entity.MensagemWhatsapp;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MensagemWhatsappRepository extends JpaRepository<MensagemWhatsapp, Long> {

    Page<MensagemWhatsapp> findByRestauranteIdAndTelefoneOrderByCriadoEmAsc(
        Long restauranteId, String telefone, Pageable pageable);

    // Deduplicação do espelho "fromMe" do webhook contra o que o
    // painel/bot já registrou na hora do envio (ver
    // MensagemWhatsappService.registrarEnviadaSeNova).
    boolean existsByRestauranteIdAndTelefoneAndDirecaoAndTextoAndCriadoEmAfter(
        Long restauranteId, String telefone, DirecaoMensagem direcao, String texto, LocalDateTime desde);

    @Query("""
        SELECT COUNT(DISTINCT m.telefone) FROM MensagemWhatsapp m
        WHERE m.restauranteId = :restauranteId AND m.direcao = :direcao AND m.lida = false
        """)
    long contarConversasNaoLidas(@Param("restauranteId") Long restauranteId, @Param("direcao") DirecaoMensagem direcao);

    @Query("""
        SELECT DISTINCT m.telefone FROM MensagemWhatsapp m
        WHERE m.restauranteId = :restauranteId AND m.direcao = :direcao AND m.lida = false
        """)
    List<String> listarTelefonesComNaoLidas(@Param("restauranteId") Long restauranteId, @Param("direcao") DirecaoMensagem direcao);

    @Modifying
    @Query("""
        UPDATE MensagemWhatsapp m SET m.lida = true
        WHERE m.restauranteId = :restauranteId AND m.telefone = :telefone AND m.direcao = :direcao AND m.lida = false
        """)
    void marcarComoLidas(@Param("restauranteId") Long restauranteId, @Param("telefone") String telefone, @Param("direcao") DirecaoMensagem direcao);

    // Uma linha por telefone, contendo a última mensagem trocada, ordenada pela mais recente.
    @Query("""
        SELECT m FROM MensagemWhatsapp m
        WHERE m.criadoEm = (
            SELECT MAX(m2.criadoEm) FROM MensagemWhatsapp m2
            WHERE m2.telefone = m.telefone AND m2.restauranteId = m.restauranteId
        )
        AND m.restauranteId = :restauranteId
        ORDER BY m.criadoEm DESC
        """)
    List<MensagemWhatsapp> findUltimaMensagemPorTelefone(@Param("restauranteId") Long restauranteId);
}
