package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.Restaurante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {
    Optional<Restaurante> findBySlug(String slug);

    // Lista leve pro painel do gestor (só identificação) — sem carregar
    // logo/background em base64, que pesam muito por restaurante.
    interface RestauranteResumo {
        Long getId();
        String getNome();
        String getSlug();
        Boolean getAtivo();
    }

    @org.springframework.data.jpa.repository.Query("SELECT r.id AS id, r.nome AS nome, r.slug AS slug, r.ativo AS ativo FROM Restaurante r ORDER BY r.nome")
    java.util.List<RestauranteResumo> listarResumos();
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsByNomeIgnoreCase(String nome);
    Optional<Restaurante> findByWhatsappInstanceName(String whatsappInstanceName);
}
