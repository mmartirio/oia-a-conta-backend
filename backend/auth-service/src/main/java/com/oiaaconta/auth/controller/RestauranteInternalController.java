package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.repository.RestauranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/restaurantes")
@RequiredArgsConstructor
public class RestauranteInternalController {

    private final RestauranteRepository restauranteRepository;

    @GetMapping("/by-instance/{instanceName}")
    public ResponseEntity<Map<String, Object>> findByInstanceName(@PathVariable String instanceName) {
        return restauranteRepository.findByWhatsappInstanceName(instanceName)
            .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                "restauranteId", r.getId(),
                "bloqueado", r.isBloqueado(),
                "ativo", r.isAtivo()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @SuppressWarnings("null")
    @GetMapping("/{id}/whatsapp-instance")
    public ResponseEntity<Map<String, Object>> getWhatsappInstance(@PathVariable Long id) {
        return restauranteRepository.findById(id)
            .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                "instanceName", r.getWhatsappInstanceName() != null ? r.getWhatsappInstanceName() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @SuppressWarnings("null")
    @GetMapping("/{id}/slug")
    public ResponseEntity<Map<String, Object>> getSlug(@PathVariable Long id) {
        return restauranteRepository.findById(id)
            .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                "slug", r.getSlug() != null ? r.getSlug() : "",
                "nome", r.getNome() != null ? r.getNome() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/localizacao")
    public ResponseEntity<RestauranteLocalizacaoInternalResponse> getLocalizacao(@PathVariable Long id) {
        return restauranteRepository.findById(id)
            .map(r -> ResponseEntity.ok(new RestauranteLocalizacaoInternalResponse(r.getLatitude(), r.getLongitude())))
            .orElse(ResponseEntity.notFound().build());
    }

    // Double (não String) — latitude/longitude nulos viram `null` no JSON em
    // vez de vazio, o que o Map.of(...) usado no resto deste controller não
    // suporta (lança NPE em valor nulo).
    public record RestauranteLocalizacaoInternalResponse(Double latitude, Double longitude) {}

    @SuppressWarnings("null")
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long id) {
        return restauranteRepository.findById(id)
            .map(r -> ResponseEntity.ok(Map.<String, Object>of(
                "bloqueado", r.isBloqueado(),
                "ativo", r.isAtivo()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @SuppressWarnings("null")
    @PutMapping("/{id}/whatsapp-instance")
    public ResponseEntity<Void> salvarInstanciaWhatsapp(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        restauranteRepository.findById(id).ifPresent(r -> {
            r.setWhatsappInstanceName(body.get("instanceName"));
            restauranteRepository.save(r);
        });
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("null")
    @PutMapping("/{id}/bloquear")
    public ResponseEntity<Void> bloquear(@PathVariable Long id) {
        restauranteRepository.findById(id).ifPresent(r -> {
            r.setBloqueado(true);
            r.setDataBloqueio(java.time.LocalDateTime.now());
            restauranteRepository.save(r);
        });
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("null")
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id) {
        restauranteRepository.findById(id).ifPresent(r -> {
            r.setBloqueado(false);
            r.setDataBloqueio(null);
            restauranteRepository.save(r);
        });
        return ResponseEntity.ok().build();
    }
}
