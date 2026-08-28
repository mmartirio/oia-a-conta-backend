package com.oiaaconta.auth.controller;

import com.oiaaconta.auth.dto.request.GoogleLoginRequest;
import com.oiaaconta.auth.dto.request.LoginRequest;
import com.oiaaconta.auth.dto.request.RegistroRequest;
import com.oiaaconta.auth.dto.response.AuthResponse;
import com.oiaaconta.auth.dto.response.UsuarioResponse;
import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.UsuarioRepository;
import com.oiaaconta.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
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
        return ResponseEntity.ok(authService.registro(request));
    }

    /** Passo 1: iniciar registro com envio de código */
    @PostMapping("/registro-iniciar")
    public ResponseEntity<Map<String, String>> registroIniciar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(202).body(authService.registroIniciar(request));
    }

    /** Passo 2: verificar código e criar conta */
    @PostMapping("/verificar-email")
    public ResponseEntity<AuthResponse> verificarEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        if (email == null || codigo == null) {
            throw new com.oiaaconta.auth.exception.BusinessException("email e codigo são obrigatórios");
        }
        return ResponseEntity.ok(authService.verificarEmail(email, codigo));
    }

    /** Reenviar código de verificação */
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Map<String, String>> reenviarCodigo(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) {
            throw new com.oiaaconta.auth.exception.BusinessException("email é obrigatório");
        }
        return ResponseEntity.ok(authService.reenviarCodigo(email));
    }

    @GetMapping("/publico/restaurante/{slug}")
    public ResponseEntity<Map<String, Object>> getRestaurantePublico(@PathVariable String slug) {
        return authService.buscarRestaurantePorSlug(slug)
            .map(r -> ResponseEntity.ok(r))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginComGoogle(request.getCredential()));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
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
            .grupoId(usuario.getGrupo() != null ? usuario.getGrupo().getId() : null)
            .grupoNome(usuario.getGrupo() != null ? usuario.getGrupo().getNome() : null)
            .permissoes(authService.permissoesEfetivas(usuario))
            .build());
    }
}
