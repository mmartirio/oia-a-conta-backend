package com.comandadigital.auth.controller;

import com.comandadigital.auth.dto.request.LoginRequest;
import com.comandadigital.auth.dto.request.RegistroRequest;
import com.comandadigital.auth.dto.response.AuthResponse;
import com.comandadigital.auth.dto.response.UsuarioResponse;
import com.comandadigital.auth.entity.Usuario;
import com.comandadigital.auth.exception.ResourceNotFoundException;
import com.comandadigital.auth.repository.UsuarioRepository;
import com.comandadigital.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(201).body(authService.registro(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginGoogle(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String nome = body.get("nome");
        return ResponseEntity.ok(authService.loginComGoogle(email, nome));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return ResponseEntity.ok(UsuarioResponse.builder()
            .id(usuario.getId())
            .nome(usuario.getNome())
            .email(usuario.getEmail())
            .role(usuario.getRole().name())
            .ativo(usuario.isAtivo())
            .restauranteId(usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null)
            .createdAt(usuario.getCreatedAt())
            .build());
    }
}
