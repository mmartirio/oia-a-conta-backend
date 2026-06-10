package com.comandadigital.auth.repository;

import com.comandadigital.auth.entity.Usuario;
import com.comandadigital.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRestauranteIdAndAtivoTrue(Long restauranteId);
    List<Usuario> findByRestauranteIdAndRoleAndAtivoTrue(Long restauranteId, Role role);
    boolean existsByEmail(String email);
}
