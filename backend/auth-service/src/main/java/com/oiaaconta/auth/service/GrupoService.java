package com.oiaaconta.auth.service;

import com.oiaaconta.auth.dto.request.GrupoRequest;
import com.oiaaconta.auth.dto.response.GrupoResponse;
import com.oiaaconta.auth.entity.Grupo;
import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.GrupoRepository;
import com.oiaaconta.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<GrupoResponse> listar(@NonNull Long restauranteId) {
        return grupoRepository.findByRestauranteIdOrderByNomeAsc(restauranteId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public GrupoResponse criar(@NonNull Long restauranteId, GrupoRequest request) {
        Grupo grupo = grupoRepository.save(Grupo.builder()
            .restauranteId(restauranteId)
            .nome(request.getNome().trim())
            .permissoes(normalizar(request.getPermissoes()))
            .build());
        return toResponse(grupo);
    }

    @Transactional
    public GrupoResponse atualizar(@NonNull Long restauranteId, @NonNull Long id, GrupoRequest request) {
        Grupo grupo = find(restauranteId, id);
        grupo.setNome(request.getNome().trim());
        grupo.setPermissoes(normalizar(request.getPermissoes()));
        return toResponse(grupoRepository.save(grupo));
    }

    // Exclui o grupo e desatribui os usuários que o tinham (voltam a
    // depender só do role fixo — nunca ficam sem conseguir logar). Grupos
    // padrão (Administrador/Garçom/Cozinheiro/Entregador) não podem ser
    // excluídos, só editados.
    @Transactional
    public long excluir(@NonNull Long restauranteId, @NonNull Long id) {
        Grupo grupo = find(restauranteId, id);
        if (grupo.isPadrao()) {
            throw new BusinessException("Grupos padrão não podem ser excluídos");
        }
        List<Usuario> usuarios = usuarioRepository.findByGrupoId(grupo.getId());
        usuarios.forEach(u -> u.setGrupo(null));
        usuarioRepository.saveAll(usuarios);
        grupoRepository.delete(grupo);
        return usuarios.size();
    }

    // Chamado no cadastro de um novo restaurante (AuthService) — dá um
    // ponto de partida pronto de grupos, sem impedir a criação de outros.
    @Transactional
    public Grupo criarGruposPadrao(@NonNull Long restauranteId) {
        Grupo administrador = grupoRepository.save(Grupo.builder()
            .restauranteId(restauranteId).nome("Administrador").padrao(true)
            .permissoes(new HashSet<>(Permissao.CHAVES))
            .build());
        grupoRepository.save(Grupo.builder()
            .restauranteId(restauranteId).nome("Garçom").padrao(true)
            .permissoes(new HashSet<>(Set.of("GARCOM", "COMANDA", "DELIVERY")))
            .build());
        grupoRepository.save(Grupo.builder()
            .restauranteId(restauranteId).nome("Cozinheiro").padrao(true)
            .permissoes(new HashSet<>(Set.of("COZINHA")))
            .build());
        grupoRepository.save(Grupo.builder()
            .restauranteId(restauranteId).nome("Entregador").padrao(true)
            .permissoes(new HashSet<>(Set.of("ENTREGADOR")))
            .build());
        return administrador;
    }

    private Grupo find(Long restauranteId, Long id) {
        return grupoRepository.findByIdAndRestauranteId(id, restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Grupo não encontrado"));
    }

    private Set<String> normalizar(Set<String> permissoes) {
        if (permissoes == null) return new HashSet<>();
        Set<String> validas = new HashSet<>();
        for (String p : permissoes) {
            if (p == null || p.isBlank()) continue;
            String chave = p.trim().toUpperCase();
            if (!Permissao.CHAVES.contains(chave)) {
                throw new BusinessException("Permissão desconhecida: " + chave);
            }
            validas.add(chave);
        }
        return validas;
    }

    private GrupoResponse toResponse(Grupo g) {
        return GrupoResponse.builder()
            .id(g.getId())
            .nome(g.getNome())
            .permissoes(g.getPermissoes())
            .totalUsuarios(usuarioRepository.countByGrupoId(g.getId()))
            .padrao(g.isPadrao())
            .createdAt(g.getCreatedAt())
            .build();
    }
}
