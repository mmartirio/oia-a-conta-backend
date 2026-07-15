package com.oiaaconta.auth.service;

import com.oiaaconta.auth.dto.request.UsuarioRequest;
import com.oiaaconta.auth.dto.response.UsuarioResponse;
import com.oiaaconta.auth.entity.Grupo;
import com.oiaaconta.auth.entity.Restaurante;
import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.enums.Role;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.GrupoRepository;
import com.oiaaconta.auth.repository.RestauranteRepository;
import com.oiaaconta.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S2245")
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<UsuarioResponse> listarPorRestaurante(@NonNull Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public List<UsuarioResponse> listarAdminsGlobal() {
        return usuarioRepository.findByRoleAndAtivoTrue(Role.ADMIN)
            .stream().map(this::toResponse).toList();
    }

    public List<UsuarioResponse> listarPorRestauranteGlobal(@NonNull Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public String resetSenha(@NonNull Long userId) {
        Usuario usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (usuario.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessException("Não é permitido resetar senha de SUPER_ADMIN");
        }
        String novaSenha = gerarSenhaTemporaria();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        emailService.enviarSenhaTemporaria(usuario.getEmail(), usuario.getNome(), novaSenha);
        return novaSenha;
    }

    @SuppressWarnings("null")
    public UsuarioResponse criar(@NonNull Long restauranteId, UsuarioRequest request) {
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
            .grupo(buscarGrupo(restauranteId, request.getGrupoId()))
            .nome(request.getNome())
            .email(request.getEmail())
            .senha(passwordEncoder.encode(
                request.getSenha() != null ? request.getSenha() : gerarSenhaTemporaria()))
            .role(request.getRole())
            .ativo(true)
            .build());

        return toResponse(usuario);
    }

    public UsuarioResponse atualizar(@NonNull Long restauranteId, @NonNull Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRestaurante() != null && restauranteId.equals(u.getRestaurante().getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        usuario.setNome(request.getNome());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        if (request.getRole() != Role.SUPER_ADMIN) {
            usuario.setRole(request.getRole());
        }
        usuario.setGrupo(buscarGrupo(restauranteId, request.getGrupoId()));

        return toResponse(usuarioRepository.save(usuario));
    }

    private Grupo buscarGrupo(Long restauranteId, Long grupoId) {
        if (grupoId == null) return null;
        return grupoRepository.findByIdAndRestauranteId(grupoId, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado"));
    }

    public void desativar(@NonNull Long restauranteId, @NonNull Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRestaurante() != null && restauranteId.equals(u.getRestaurante().getId()))
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
            .grupoId(u.getGrupo() != null ? u.getGrupo().getId() : null)
            .grupoNome(u.getGrupo() != null ? u.getGrupo().getNome() : null)
            .build();
    }

    private String gerarSenhaTemporaria() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(rng.nextInt(chars.length())));
        return sb.toString();
    }
}
