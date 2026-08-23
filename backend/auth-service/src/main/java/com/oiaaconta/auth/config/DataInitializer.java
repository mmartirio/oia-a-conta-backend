package com.oiaaconta.auth.config;

import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.enums.Role;
import com.oiaaconta.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Só usado na primeira subida (banco vazio), pra existir uma conta
    // SUPER_ADMIN inicial. Sobrescrevível via env var em produção — a senha
    // nunca deve ir pro log (docker logs fica acessível a qualquer um com
    // acesso ao host/container).
    @Value("${SUPER_ADMIN_SENHA:SuperAdmin@123}")
    private String superAdminSenha;

    @Bean
    @SuppressWarnings("null")
    public ApplicationRunner initSuperAdmin() {
        return args -> {
            if (!usuarioRepository.existsByEmail("superadmin@comanda.digital")) {
                Usuario superAdmin = Usuario.builder()
                    .nome("Super Admin")
                    .email("superadmin@comanda.digital")
                    .senha(passwordEncoder.encode(superAdminSenha))
                    .role(Role.SUPER_ADMIN)
                    .ativo(true)
                    .emailVerificado(true)
                    .build();
                usuarioRepository.save(superAdmin);
                log.info("Super Admin criado (superadmin@comanda.digital) — senha definida via SUPER_ADMIN_SENHA ou o padrão de desenvolvimento; troque-a após o primeiro login.");
            }
        };
    }
}
