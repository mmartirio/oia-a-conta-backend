package com.oiaaconta.auth.security;

import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        // Grupo (se atribuído) substitui completamente o role fixo pra
        // fins de autorização — ver PermissaoRoles.
        String[] roles = u.getGrupo() != null
            ? PermissaoRoles.derivar(u.getGrupo().getPermissoes()).toArray(new String[0])
            : new String[]{ u.getRole().name() };

        return User.builder()
            .username(u.getEmail())
            .password(u.getSenha())
            .roles(roles)
            .build();
    }
}
