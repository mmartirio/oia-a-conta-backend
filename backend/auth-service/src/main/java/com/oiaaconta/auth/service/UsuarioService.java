package com.oiaaconta.auth.service;

import com.oiaaconta.auth.client.AuditoriaClient;
import com.oiaaconta.auth.dto.request.CriarSuperAdminRequest;
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
    private final AuditoriaService auditoriaService;
    private final AuditoriaClient billingClient;

    public List<UsuarioResponse> listarPorRestaurante(@NonNull Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    public List<UsuarioResponse> listarAdminsGlobal() {
        return usuarioRepository.findByRoleAndAtivoTrue(Role.ADMIN)
            .stream().map(this::toResponse).toList();
    }

    public List<UsuarioResponse> listarSuperAdmins() {
        return usuarioRepository.findByRole(Role.SUPER_ADMIN)
            .stream().map(this::toResponse).toList();
    }

    @SuppressWarnings("null")
    public UsuarioResponse criarSuperAdmin(CriarSuperAdminRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
            .nome(request.getNome())
            .email(request.getEmail())
            .senha(passwordEncoder.encode(
                request.getSenha() != null ? request.getSenha() : gerarSenhaTemporaria()))
            .role(Role.SUPER_ADMIN)
            .ativo(true)
            .emailVerificado(true)
            .build());
        return toResponse(usuario);
    }

    public UsuarioResponse alternarAtivoSuperAdmin(@NonNull Long id, boolean ativo) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRole() == Role.SUPER_ADMIN)
            .orElseThrow(() -> new ResourceNotFoundException("SUPER_ADMIN não encontrado"));
        if (!ativo && usuarioRepository.findByRoleAndAtivoTrue(Role.SUPER_ADMIN).size() <= 1) {
            throw new BusinessException("Não é possível desativar o único SUPER_ADMIN ativo");
        }
        usuario.setAtivo(ativo);
        return toResponse(usuarioRepository.save(usuario));
    }

    @SuppressWarnings("null")
    public UsuarioResponse atualizarSuperAdmin(@NonNull Long id, CriarSuperAdminRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRole() == Role.SUPER_ADMIN)
            .orElseThrow(() -> new ResourceNotFoundException("SUPER_ADMIN não encontrado"));
        if (!usuario.getEmail().equals(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
        }
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        }
        return toResponse(usuarioRepository.save(usuario));
    }

    public void excluirSuperAdmin(@NonNull Long id) {
        Usuario usuario = usuarioRepository.findById(id)
            .filter(u -> u.getRole() == Role.SUPER_ADMIN)
            .orElseThrow(() -> new ResourceNotFoundException("SUPER_ADMIN não encontrado"));
        if (usuario.isAtivo() && usuarioRepository.findByRoleAndAtivoTrue(Role.SUPER_ADMIN).size() <= 1) {
            throw new BusinessException("Não é possível excluir o único SUPER_ADMIN ativo");
        }
        usuarioRepository.delete(usuario);
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
        verificarLimiteUsuarios(restauranteId);

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
            .emailVerificado(true)
            .build());

        auditoriaService.registrar(restauranteId, "USUARIO_CRIADO",
            "Usuário " + usuario.getNome() + " (" + usuario.getEmail() + ") criado como " + usuario.getRole(),
            usuario.getId(), usuario.getNome());

        return toResponse(usuario);
    }

    // Sem contrato/plano ou com o billing-service fora do ar, não bloqueia
    // (best-effort) — o dono do restaurante não pode ficar impedido de criar
    // usuário por uma falha de infraestrutura alheia à conta dele.
    private void verificarLimiteUsuarios(Long restauranteId) {
        Integer limite;
        try {
            limite = billingClient.buscarLimitesPlano(restauranteId).getLimiteUsuarios();
        } catch (Exception e) {
            return;
        }
        if (limite == null) return;
        long ativos = usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId).size();
        if (ativos >= limite) {
            throw new BusinessException(
                "Limite de " + limite + " usuário(s) do plano contratado atingido. Faça upgrade do plano para adicionar mais usuários.");
        }
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
        if (usuario.isDonoConta()) {
            Long grupoAtualId = usuario.getGrupo() != null ? usuario.getGrupo().getId() : null;
            if (!java.util.Objects.equals(grupoAtualId, request.getGrupoId())) {
                throw new BusinessException("O dono do estabelecimento não pode ser removido do grupo Administrador");
            }
        } else {
            usuario.setGrupo(buscarGrupo(restauranteId, request.getGrupoId()));
        }

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

        auditoriaService.registrar(restauranteId, "USUARIO_REMOVIDO",
            "Usuário " + usuario.getNome() + " (" + usuario.getEmail() + ") desativado",
            usuario.getId(), usuario.getNome());
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
            .donoConta(u.isDonoConta())
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
