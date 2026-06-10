package com.comandadigital.auth.security;

import com.comandadigital.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
            .map(u -> User.builder()
                .username(u.getEmail())
                .password(u.getSenha())
                .roles(u.getRole().name())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }
}
