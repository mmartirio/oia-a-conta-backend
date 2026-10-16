package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.repository.RestauranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Identificação dos restaurantes pro painel do gestor (SUPER_ADMIN) — os
// contratos (billing-service) só têm restauranteId; o nome mora aqui.
@RestController
@RequestMapping("/api/restaurantes")
@RequiredArgsConstructor
public class GestorRestauranteController {

    private final RestauranteRepository restauranteRepository;

    @GetMapping("/nomes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listarNomes() {
        return ResponseEntity.ok(restauranteRepository.listarResumos().stream()
            .map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "nome", r.getNome() != null ? r.getNome() : "",
                "slug", r.getSlug() != null ? r.getSlug() : "",
                "ativo", Boolean.TRUE.equals(r.getAtivo())))
            .toList());
    }

    // Dados cadastrais de uma empresa (tela de detalhe do gestor) — sem
    // logo/background (base64 pesado) nem configurações visuais.
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> buscar(@PathVariable Long id) {
        return restauranteRepository.findById(id)
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("nome", r.getNome());
                m.put("slug", r.getSlug());
                m.put("cnpj", r.getCnpj());
                m.put("telefone", r.getTelefone());
                m.put("emailResponsavel", r.getEmailResponsavel());
                m.put("enderecoRua", r.getEnderecoRua());
                m.put("enderecoNumero", r.getEnderecoNumero());
                m.put("enderecoBairro", r.getEnderecoBairro());
                m.put("enderecoCidade", r.getEnderecoCidade());
                m.put("enderecoComplemento", r.getEnderecoComplemento());
                m.put("ativo", r.isAtivo());
                m.put("dataBloqueio", r.getDataBloqueio());
                m.put("whatsappConfigurado", r.getWhatsappInstanceName() != null);
                m.put("createdAt", r.getCreatedAt());
                return ResponseEntity.ok(m);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
