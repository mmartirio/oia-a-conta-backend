package com.oiaaconta.auth.repository;

import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.enums.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // grupo (LAZY) precisa vir junto — JwtAuthFilter acessa
    // usuario.getGrupo().getPermissoes() fora de uma transação (é um Filter
    // puro, sem @Transactional), então sem isso um usuário COM grupo
    // atribuído derruba toda chamada autenticada com
    // LazyInitializationException assim que o Hibernate tenta inicializar o
    // proxy do grupo sem sessão aberta.
    @EntityGraph(attributePaths = "grupo")
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRestauranteIdAndAtivoTrue(Long restauranteId);
    List<Usuario> findByRestauranteIdAndRoleAndAtivoTrue(Long restauranteId, Role role);
    List<Usuario> findByRoleAndAtivoTrue(Role role);
    List<Usuario> findByRole(Role role);
    boolean existsByEmail(String email);
    long countByGrupoId(Long grupoId);
    List<Usuario> findByGrupoId(Long grupoId);
}
