package com.oiaaconta.auth.service;

import com.oiaaconta.auth.dto.request.LoginRequest;
import com.oiaaconta.auth.dto.request.RegistroRequest;
import com.oiaaconta.auth.dto.response.AuthResponse;
import com.oiaaconta.auth.entity.EmailVerificacao;
import com.oiaaconta.auth.entity.RegistroPendente;
import com.oiaaconta.auth.entity.Restaurante;
import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.enums.Role;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.EmailVerificacaoRepository;
import com.oiaaconta.auth.repository.RegistroPendenteRepository;
import com.oiaaconta.auth.repository.RestauranteRepository;
import com.oiaaconta.auth.repository.UsuarioRepository;
import com.oiaaconta.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final EmailVerificacaoRepository emailVerificacaoRepository;
    private final RegistroPendenteRepository registroPendenteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    @Value("${evolution.api.url:http://oia-evolution:8080}")
    private String evolutionUrl;

    @Value("${evolution.api.key:oia_evolution_key_2024}")
    private String evolutionKey;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ─── Login ───────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (!usuario.isEmailVerificado()) {
            throw new BusinessException("E-mail não verificado. Verifique sua caixa de entrada.");
        }
        return buildAuthResponse(usuario);
    }

    // ─── Registro 2 etapas ───────────────────────────────────────────────────

    @Transactional
    public Map<String, String> registroIniciar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado");
        }

        // Remove registro pendente anterior (se existir) e cria novo
        registroPendenteRepository.findByEmail(request.getEmail())
            .ifPresent(registroPendenteRepository::delete);
        registroPendenteRepository.flush();

        RegistroPendente pendente = RegistroPendente.builder()
            .nomeRestaurante(request.getNomeRestaurante())
            .nomeAdmin(request.getNomeAdmin())
            .email(request.getEmail())
            .senhaHash(passwordEncoder.encode(request.getSenha()))
            .cnpj(request.getCnpj())
            .telefone(request.getTelefone())
            .planoId(request.getPlanoId())
            .expiradoEm(LocalDateTime.now().plusHours(1))
            .build();
        registroPendenteRepository.save(pendente);

        enviarCodigo(request.getEmail(), request.getNomeAdmin());
        return Map.of("mensagem", "Código de verificação enviado para " + request.getEmail());
    }

    @Transactional
    @SuppressWarnings("null")
    public AuthResponse verificarEmail(String email, String codigo) {
        EmailVerificacao verificacao = emailVerificacaoRepository
            .findTopByEmailAndUsadoFalseOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new BusinessException("Código inválido ou expirado"));

        if (!verificacao.isValido() || !verificacao.getCodigo().equals(codigo)) {
            throw new BusinessException("Código inválido ou expirado");
        }

        verificacao.setUsado(true);
        emailVerificacaoRepository.save(verificacao);

        // Procura registro pendente para criar conta
        return registroPendenteRepository.findByEmail(email)
            .map(pendente -> {
                AuthResponse response = criarContaFromPendente(pendente);
                registroPendenteRepository.delete(pendente);
                return response;
            })
            .orElseGet(() -> {
                // Verificação de usuário existente (criado por admin)
                Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
                usuario.setEmailVerificado(true);
                usuarioRepository.save(usuario);
                return buildAuthResponse(usuario);
            });
    }

    @Transactional
    public Map<String, String> reenviarCodigo(String email) {
        boolean temPendente = registroPendenteRepository.findByEmail(email).isPresent();
        boolean temUsuario = usuarioRepository.findByEmail(email)
            .map(u -> !u.isEmailVerificado()).orElse(false);

        if (!temPendente && !temUsuario) {
            throw new BusinessException("E-mail não encontrado ou já verificado");
        }

        String nome = usuarioRepository.findByEmail(email)
            .map(Usuario::getNome)
            .orElseGet(() -> registroPendenteRepository.findByEmail(email)
                .map(RegistroPendente::getNomeAdmin).orElse("usuário"));

        enviarCodigo(email, nome);
        return Map.of("mensagem", "Novo código enviado para " + email);
    }

    // ─── Registro direto (1 etapa) ───────────────────────────────────────────

    @Transactional
    @SuppressWarnings("null")
    public AuthResponse registro(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                "Este e-mail já está cadastrado. Faça login ou utilize outro e-mail.");
        }
        if (restauranteRepository.existsByNomeIgnoreCase(request.getNomeRestaurante())) {
            throw new BusinessException(
                "Já existe uma empresa com o nome \"" + request.getNomeRestaurante() +
                "\" cadastrada. Verifique o nome ou entre em contato com o suporte.");
        }

        String slug = generateSlug(request.getNomeRestaurante());
        if (restauranteRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Restaurante restaurante = restauranteRepository.save(
            Restaurante.builder()
                .nome(request.getNomeRestaurante())
                .slug(slug)
                .emailResponsavel(request.getEmail())
                .cnpj(request.getCnpj())
                .telefone(request.getTelefone())
                .plano("BASICO")
                .ativo(true)
                .build()
        );

        Usuario admin = usuarioRepository.save(
            Usuario.builder()
                .restaurante(restaurante)
                .nome(request.getNomeAdmin())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .role(Role.ADMIN)
                .ativo(true)
                .emailVerificado(true)
                .build()
        );

        emailService.enviarBoasVindas(request.getEmail(), request.getNomeAdmin(), restaurante.getNome());
        criarInstanciaWhatsapp(restaurante);
        criarContratoBilling(restaurante.getId(), request.getPlanoId());
        return buildAuthResponse(admin);
    }

    // ─── Público ─────────────────────────────────────────────────────────────

    public java.util.Optional<java.util.Map<String, Object>> buscarRestaurantePorSlug(String slug) {
        return restauranteRepository.findBySlug(slug)
            .map(r -> java.util.Map.<String, Object>of(
                "id", r.getId(),
                "nome", r.getNome(),
                "slug", r.getSlug()
            ));
    }

    // ─── Google ──────────────────────────────────────────────────────────────

    @Transactional
    @SuppressWarnings("null")
    public AuthResponse loginComGoogle(String googleEmail, String googleNome) {
        return usuarioRepository.findByEmail(googleEmail)
            .map(this::buildAuthResponse)
            .orElseGet(() -> {
                String slug = generateSlug(googleNome != null ? googleNome : googleEmail.split("@")[0]);
                if (restauranteRepository.existsBySlug(slug)) {
                    slug = slug + "-" + System.currentTimeMillis();
                }
                Restaurante restaurante = restauranteRepository.save(
                    Restaurante.builder()
                        .nome(googleNome != null ? googleNome + " - Restaurante" : "Meu Restaurante")
                        .slug(slug)
                        .emailResponsavel(googleEmail)
                        .plano("BASICO")
                        .ativo(true)
                        .build()
                );
                Usuario usuario = usuarioRepository.save(
                    Usuario.builder()
                        .restaurante(restaurante)
                        .nome(googleNome != null ? googleNome : googleEmail.split("@")[0])
                        .email(googleEmail)
                        .senha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .role(Role.ADMIN)
                        .ativo(true)
                        .emailVerificado(true) // Google já valida o e-mail
                        .build()
                );
                emailService.enviarBoasVindas(googleEmail, usuario.getNome(), restaurante.getNome());
                criarInstanciaWhatsapp(restaurante);
                return buildAuthResponse(usuario);
            });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("null")
    private AuthResponse criarContaFromPendente(RegistroPendente pendente) {
        String slug = generateSlug(pendente.getNomeRestaurante());
        if (restauranteRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        Restaurante restaurante = restauranteRepository.save(
            Restaurante.builder()
                .nome(pendente.getNomeRestaurante())
                .slug(slug)
                .emailResponsavel(pendente.getEmail())
                .cnpj(pendente.getCnpj())
                .telefone(pendente.getTelefone())
                .plano("BASICO")
                .ativo(true)
                .build()
        );
        Usuario admin = usuarioRepository.save(
            Usuario.builder()
                .restaurante(restaurante)
                .nome(pendente.getNomeAdmin())
                .email(pendente.getEmail())
                .senha(pendente.getSenhaHash())
                .role(Role.ADMIN)
                .ativo(true)
                .emailVerificado(true)
                .build()
        );
        emailService.enviarBoasVindas(pendente.getEmail(), pendente.getNomeAdmin(), restaurante.getNome());
        criarInstanciaWhatsapp(restaurante);
        criarContratoBilling(restaurante.getId(), pendente.getPlanoId());
        return buildAuthResponse(admin);
    }

    private void criarInstanciaWhatsapp(Restaurante restaurante) {
        String instanceName = "oia-" + restaurante.getId();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", evolutionKey);
            Map<String, Object> body = Map.of(
                "instanceName", instanceName,
                "integration", "WHATSAPP-BAILEYS",
                "qrcode", true
            );
            restTemplate.postForEntity(
                evolutionUrl + "/instance/create",
                new HttpEntity<>(body, headers),
                Object.class
            );
            restaurante.setWhatsappInstanceName(instanceName);
            restauranteRepository.save(restaurante);
            log.info("Instância WhatsApp '{}' criada para restaurante {}", instanceName, restaurante.getId());
        } catch (Exception e) {
            log.warn("Não foi possível criar instância WhatsApp para restaurante {}: {}", restaurante.getId(), e.getMessage());
        }
    }

    private void criarContratoBilling(Long restauranteId, Long planoId) {
        if (planoId == null) return;
        try {
            restTemplate.postForObject(
                "http://billing-service/internal/contratos",
                Map.of("restauranteId", restauranteId, "planoId", planoId),
                Object.class
            );
        } catch (Exception e) {
            log.warn("Não foi possível criar contrato no billing-service: {}", e.getMessage());
        }
    }

    @SuppressWarnings("null")
    private void enviarCodigo(String email, String nome) {
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        emailVerificacaoRepository.invalidarTodosPorEmail(email);
        emailVerificacaoRepository.save(EmailVerificacao.builder()
            .email(email)
            .codigo(codigo)
            .expiradoEm(LocalDateTime.now().plusMinutes(15))
            .build());
        emailService.enviarCodigoVerificacao(email, nome, codigo);
        emailService.logCodigoDesenvolvimento(email, codigo);
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        String token = jwtUtil.generateToken(usuario);
        return AuthResponse.builder()
            .token(token)
            .usuario(AuthResponse.UsuarioDto.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole().name())
                .restauranteId(usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null)
                .ativo(usuario.isAtivo())
                .build())
            .build();
    }

    private String generateSlug(String nome) {
        String normalized = Normalizer.normalize(nome, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized)
            .replaceAll("")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("[\\s]+", "-")
            .replaceAll("-+", "-")
            .trim();
    }
}
