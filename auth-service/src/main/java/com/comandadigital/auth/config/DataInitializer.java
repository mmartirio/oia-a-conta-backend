package com.comandadigital.auth.config;

import com.comandadigital.auth.entity.Restaurante;
import com.comandadigital.auth.entity.Usuario;
import com.comandadigital.auth.enums.Role;
import com.comandadigital.auth.repository.RestauranteRepository;
import com.comandadigital.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initSuperAdmin() {
        return args -> {
            if (!usuarioRepository.existsByEmail("superadmin@comanda.digital")) {
                Usuario superAdmin = Usuario.builder()
                    .nome("Super Admin")
                    .email("superadmin@comanda.digital")
                    .senha(passwordEncoder.encode("SuperAdmin@123"))
                    .role(Role.SUPER_ADMIN)
                    .ativo(true)
                    .build();
                usuarioRepository.save(superAdmin);
                log.info("Super Admin criado: superadmin@comanda.digital / SuperAdmin@123");
            }
        };
    }
}
