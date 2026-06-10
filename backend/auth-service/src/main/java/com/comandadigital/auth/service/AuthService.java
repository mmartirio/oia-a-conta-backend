package com.comandadigital.auth.service;

import com.comandadigital.auth.dto.request.LoginRequest;
import com.comandadigital.auth.dto.request.RegistroRequest;
import com.comandadigital.auth.dto.response.AuthResponse;
import com.comandadigital.auth.entity.Restaurante;
import com.comandadigital.auth.entity.Usuario;
import com.comandadigital.auth.enums.Role;
import com.comandadigital.auth.exception.BusinessException;
import com.comandadigital.auth.exception.ResourceNotFoundException;
import com.comandadigital.auth.repository.RestauranteRepository;
import com.comandadigital.auth.repository.UsuarioRepository;
import com.comandadigital.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return buildAuthResponse(usuario);
    }

    @Transactional
    public AuthResponse registro(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
        }

        String slug = generateSlug(request.getNomeRestaurante());
        if (restauranteRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Restaurante restaurante = restauranteRepository.save(
            Restaurante.builder()
                .nome(request.getNomeRestaurante())
                .slug(slug)
                .plano("BASICO")
                .ativo(true)
                .build()
        );

        Usuario admin = usuarioRepository.save(
            Usuario.builder()
                .restaurante(restaurante)
                .nome(request.getNomeAdmin())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .role(Role.ADMIN)
                .ativo(true)
                .build()
        );

        return buildAuthResponse(admin);
    }

    public AuthResponse loginComGoogle(String googleEmail, String googleNome) {
        return usuarioRepository.findByEmail(googleEmail)
            .map(this::buildAuthResponse)
            .orElseThrow(() -> new BusinessException(
                "Conta Google não vinculada. Registre seu restaurante ou solicite ao administrador."));
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        String token = jwtUtil.generateToken(usuario);
        return AuthResponse.builder()
            .token(token)
            .userId(usuario.getId())
            .nome(usuario.getNome())
            .email(usuario.getEmail())
            .role(usuario.getRole().name())
            .restauranteId(usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null)
            .restauranteNome(usuario.getRestaurante() != null ? usuario.getRestaurante().getNome() : null)
            .build();
    }

    private String generateSlug(String nome) {
        String normalized = Normalizer.normalize(nome, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized)
            .replaceAll("")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("[\\s]+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
