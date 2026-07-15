package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.dto.request.BackgroundRequest;
import com.oiaaconta.auth.dto.request.LogoRequest;
import com.oiaaconta.auth.dto.request.RestauranteCoresRequest;
import com.oiaaconta.auth.dto.request.RestauranteLocalizacaoRequest;
import com.oiaaconta.auth.dto.request.RestauranteUpdateRequest;
import com.oiaaconta.auth.dto.response.RestauranteDadosResponse;
import com.oiaaconta.auth.service.RestauranteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/restaurante")
@RequiredArgsConstructor
public class RestauranteController {

    private final RestauranteService restauranteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> buscarDados(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId) {
        return ResponseEntity.ok(restauranteService.buscarDados(restauranteId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> atualizarDados(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @Valid @RequestBody RestauranteUpdateRequest request) {
        return ResponseEntity.ok(restauranteService.atualizarDados(restauranteId, request));
    }

    @PutMapping("/cores")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> atualizarCores(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @RequestBody RestauranteCoresRequest request) {
        return ResponseEntity.ok(restauranteService.atualizarCores(restauranteId, request));
    }

    @PutMapping("/localizacao")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> atualizarLocalizacao(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @Valid @RequestBody RestauranteLocalizacaoRequest request) {
        return ResponseEntity.ok(restauranteService.atualizarLocalizacao(restauranteId, request));
    }

    @PutMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> atualizarLogo(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @RequestBody LogoRequest request) {
        return ResponseEntity.ok(restauranteService.atualizarLogo(restauranteId, request));
    }

    @PutMapping("/background")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<RestauranteDadosResponse> atualizarBackground(
            @RequestHeader("X-Restaurante-Id") @NonNull Long restauranteId,
            @RequestBody BackgroundRequest request) {
        return ResponseEntity.ok(restauranteService.atualizarBackground(restauranteId, request));
    }
}
