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

    // Só usado na primeira subida (nenhum SUPER_ADMIN no banco), pra existir
    // uma conta inicial. SEM valor padrão: o repositório é público, então
    // qualquer senha escrita aqui seria conhecida por todo mundo. A senha
    // nunca deve ir pro log (docker logs fica acessível a quem tem acesso ao host).
    @Value("${SUPER_ADMIN_SENHA:}")
    private String superAdminSenha;

    // Senha que já foi o padrão no código (repositório público) — qualquer
    // SUPER_ADMIN que ainda use ela está, na prática, sem senha.
    private static final String SENHA_PADRAO_ANTIGA = "SuperAdmin@123";

    // Corrige contas criadas antes desta versão: troca pela SUPER_ADMIN_SENHA
    // se estiver configurada (≥ 12 caracteres); senão, desativa a conta.
    // Nunca reativa — conta desativada de propósito continua desativada.
    private void neutralizarSenhaPadraoPublica() {
        boolean temSenhaNova = superAdminSenha != null && superAdminSenha.length() >= 12;
        for (Usuario u : usuarioRepository.findByRole(Role.SUPER_ADMIN)) {
            if (!passwordEncoder.matches(SENHA_PADRAO_ANTIGA, u.getSenha())) continue;
            if (temSenhaNova) {
                u.setSenha(passwordEncoder.encode(superAdminSenha));
                log.warn("SUPER_ADMIN {} usava a senha padrão pública — trocada pela SUPER_ADMIN_SENHA.", u.getEmail());
            } else {
                u.setAtivo(false);
                log.error("SUPER_ADMIN {} usava a senha padrão pública — DESATIVADO. Defina SUPER_ADMIN_SENHA (12+ caracteres) e reinicie pra trocar a senha (a conta continua desativada até ser reativada por outro SUPER_ADMIN).", u.getEmail());
            }
            usuarioRepository.save(u);
        }
    }

    @Bean
    @SuppressWarnings("null")
    public ApplicationRunner initSuperAdmin() {
        return args -> {
            neutralizarSenhaPadraoPublica();
            // Checa se existe QUALQUER SUPER_ADMIN (não esse e-mail específico):
            // antes, excluir o superadmin@comanda.digital pelo painel fazia ele
            // ser recriado a cada restart/deploy, com a senha padrão pública.
            if (usuarioRepository.findByRole(Role.SUPER_ADMIN).isEmpty()) {
                if (superAdminSenha == null || superAdminSenha.length() < 12) {
                    log.error("Nenhum SUPER_ADMIN no banco e SUPER_ADMIN_SENHA ausente ou com menos de 12 caracteres — conta inicial NÃO criada.");
                    return;
                }
                Usuario superAdmin = Usuario.builder()
                    .nome("Super Admin")
                    .email("superadmin@comanda.digital")
                    .senha(passwordEncoder.encode(superAdminSenha))
                    .role(Role.SUPER_ADMIN)
                    .ativo(true)
                    .emailVerificado(true)
                    .build();
                usuarioRepository.save(superAdmin);
                log.info("Super Admin inicial criado (superadmin@comanda.digital) com a senha de SUPER_ADMIN_SENHA — troque-a após o primeiro login.");
            }
        };
    }
}
