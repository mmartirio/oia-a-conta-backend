package com.comandadigital.auth.service;

import com.comandadigital.auth.dto.request.UsuarioRequest;
import com.comandadigital.auth.dto.response.UsuarioResponse;
import com.comandadigital.auth.entity.Restaurante;
import com.comandadigital.auth.entity.Usuario;
import com.comandadigital.auth.enums.Role;
import com.comandadigital.auth.exception.BusinessException;
import com.comandadigital.auth.exception.ResourceNotFoundException;
import com.comandadigital.auth.repository.RestauranteRepository;
import com.comandadigital.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listarPorRestaurante(Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public UsuarioResponse criar(Long restauranteId, UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        if (request.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException("Não é permitido criar usuário SUPER_ADMIN");
        }

        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
            .restaurante(restaurante)
            .nome(request.getNome())
            .email(request.getEmail())
            .senha(passwordEncoder.encode(
                request.getSenha() != null ? request.getSenha() : "Trocar@123"))
            .role(request.getRole())
            .ativo(true)
            .build());

        return toResponse(usuario);
    }

    public UsuarioResponse atualizar(Long restauranteId, Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRestaurante() != null && u.getRestaurante().getId().equals(restauranteId))
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        usuario.setNome(request.getNome());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        if (request.getRole() != Role.SUPER_ADMIN) {
            usuario.setRole(request.getRole());
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    public void desativar(Long restauranteId, Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRestaurante() != null && u.getRestaurante().getId().equals(restauranteId))
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
            .id(u.getId())
            .nome(u.getNome())
            .email(u.getEmail())
            .role(u.getRole().name())
            .ativo(u.isAtivo())
            .restauranteId(u.getRestaurante() != null ? u.getRestaurante().getId() : null)
            .createdAt(u.getCreatedAt())
            .build();
    }
}
