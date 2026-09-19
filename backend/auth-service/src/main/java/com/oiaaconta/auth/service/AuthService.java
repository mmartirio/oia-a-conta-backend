package com.oiaaconta.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.oiaaconta.auth.dto.request.LoginRequest;
import com.oiaaconta.auth.dto.request.RegistroRequest;
import com.oiaaconta.auth.client.AuditoriaClient;
import com.oiaaconta.auth.dto.response.AuthResponse;
import com.oiaaconta.auth.entity.AceiteContrato;
import com.oiaaconta.auth.entity.EmailVerificacao;
import com.oiaaconta.auth.entity.Grupo;
import com.oiaaconta.auth.entity.RegistroPendente;
import com.oiaaconta.auth.entity.Restaurante;
import com.oiaaconta.auth.entity.Usuario;
import com.oiaaconta.auth.enums.Role;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.AceiteContratoRepository;
import com.oiaaconta.auth.repository.EmailVerificacaoRepository;
import com.oiaaconta.auth.repository.RegistroPendenteRepository;
import com.oiaaconta.auth.repository.RestauranteRepository;
import com.oiaaconta.auth.repository.UsuarioRepository;
import com.oiaaconta.auth.security.JwtUtil;
import com.oiaaconta.auth.util.FuncionalidadePermissoes;
import com.oiaaconta.auth.util.SlugUtil;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RestauranteRepository restauranteRepository;
    private final EmailVerificacaoRepository emailVerificacaoRepository;
    private final RegistroPendenteRepository registroPendenteRepository;
    private final AceiteContratoRepository aceiteContratoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    private final GrupoService grupoService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AuditoriaService auditoriaService;
    private final LoginAttemptService loginAttemptService;
    private final AuditoriaClient billingClient;

    @Value("${evolution.api.url:http://oia-evolution:8080}")
    private String evolutionUrl;

    @Value("${evolution.api.key:oia_evolution_key_2024}")
    private String evolutionKey;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ─── Login ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        loginAttemptService.verificarBloqueio(request.getEmail());
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.registrarFalha(request.getEmail());
            throw e;
        }
        loginAttemptService.registrarSucesso(request.getEmail());
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (!usuario.isEmailVerificado()) {
            throw new BusinessException("E-mail não verificado. Verifique sua caixa de entrada.");
        }
        auditoriaService.registrar(usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null,
            "LOGIN", "Login de " + usuario.getEmail(), usuario.getId(), usuario.getNome());
        return buildAuthResponse(usuario);
    }

    // ─── Registro 2 etapas ───────────────────────────────────────────────────

    @Transactional
    public Map<String, String> registroIniciar(RegistroRequest request, String ipCliente) {
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
            .modalidadeOperacao(request.getModalidadeOperacao())
            .expiradoEm(LocalDateTime.now().plusHours(1))
            .versaoContratoAceito(request.getVersaoContrato())
            .aceitoEm(LocalDateTime.now())
            .ipAceite(ipCliente)
            .build();
        registroPendenteRepository.save(pendente);

        enviarCodigo(request.getEmail(), request.getNomeAdmin());
        return Map.of("mensagem", "Código de verificação enviado para " + request.getEmail());
    }

    private static final int MAX_TENTATIVAS_CODIGO = 5;

    // noRollbackFor é essencial aqui: sem isso, o rollback automático do
    // Spring em qualquer RuntimeException desfaria o save() do contador de
    // tentativas junto com a exceção — o contador nunca persistiria e o
    // limite de tentativas nunca dispararia de verdade.
    @Transactional(noRollbackFor = BusinessException.class)
    @SuppressWarnings("null")
    public AuthResponse verificarEmail(String email, String codigo) {
        EmailVerificacao verificacao = emailVerificacaoRepository
            .findTopByEmailAndUsadoFalseOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new BusinessException("Código inválido ou expirado"));

        if (!verificacao.isValido()) {
            throw new BusinessException("Código inválido ou expirado");
        }

        if (!verificacao.getCodigo().equals(codigo)) {
            verificacao.setTentativas(verificacao.getTentativas() + 1);
            if (verificacao.getTentativas() >= MAX_TENTATIVAS_CODIGO) {
                // Invalida de vez — sem isso, o código de 6 dígitos seria
                // brute-forceável dentro da própria janela de 15 min.
                verificacao.setUsado(true);
                emailVerificacaoRepository.save(verificacao);
                throw new BusinessException("Muitas tentativas incorretas. Solicite um novo código.");
            }
            emailVerificacaoRepository.save(verificacao);
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

        Grupo grupoAdministrador = grupoService.criarGruposPadrao(restaurante.getId());

        Usuario admin = usuarioRepository.save(
            Usuario.builder()
                .restaurante(restaurante)
                .grupo(grupoAdministrador)
                .donoConta(true)
                .nome(request.getNomeAdmin())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .role(Role.ADMIN)
                .ativo(true)
                .emailVerificado(true)
                .build()
        );

        // Endpoint legado de registro em etapa única, sem o fluxo de aceite
        // de contrato do registro-iniciar — sem dados de plano/aceite pra
        // anexar o PDF do contrato.
        emailService.enviarBoasVindas(request.getEmail(), request.getNomeAdmin(), restaurante.getNome(),
            null, null, null, null, null);
        criarInstanciaWhatsapp(restaurante);
        criarContratoBilling(restaurante.getId(), request.getPlanoId(), request.getModalidadeOperacao());
        criarCategoriasPadraoCatalogo(restaurante.getId());
        return buildAuthResponse(admin);
    }

    // ─── Público ─────────────────────────────────────────────────────────────

    public java.util.Optional<java.util.Map<String, Object>> buscarRestaurantePorSlug(String slug) {
        return restauranteRepository.findBySlug(slug)
            .map(r -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("nome", r.getNome());
                map.put("slug", r.getSlug());
                map.put("telefone", r.getTelefone() != null ? r.getTelefone() : "");
                map.put("logoUrl", r.getLogoBase64());
                map.put("corPrimaria", r.getCorPrimaria());
                map.put("corSecundaria", r.getCorSecundaria());
                map.put("corAccent", r.getCorAccent());
                map.put("corTexto", r.getCorTexto());
                map.put("backgroundUrl", r.getBackgroundBase64());
                map.put("backgroundOpacidade", r.getBackgroundOpacidade());
                return map;
            });
    }

    // ─── Google ──────────────────────────────────────────────────────────────

    // Login social: NÃO cria restaurante/usuário automaticamente. O ID token é
    // verificado (assinatura + audience) no backend — nunca confia em e-mail/
    // nome mandado em texto puro pelo cliente. Se o e-mail verificado pelo
    // Google não tiver cadastro, devolve 404 (ResourceNotFoundException, não
    // BusinessException) — é o sinal que o frontend usa pra redirecionar
    // direto pra tela de cadastro em vez de só mostrar um erro genérico.
    @Transactional(readOnly = true)
    public AuthResponse loginComGoogle(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = googleIdTokenVerifier.verify(idTokenString);
        } catch (Exception e) {
            throw new BusinessException("Não foi possível validar o login com Google. Tente novamente.");
        }
        if (idToken == null) {
            throw new BusinessException("Token do Google inválido ou expirado.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BusinessException("O e-mail da sua conta Google não está verificado.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nenhum restaurante cadastrado para este e-mail. Cadastre-se para continuar."));

        if (!usuario.isAtivo()) {
            throw new BusinessException("Usuário desativado. Entre em contato com o administrador.");
        }
        if (!usuario.isEmailVerificado()) {
            throw new BusinessException("E-mail não verificado. Verifique sua caixa de entrada.");
        }

        return buildAuthResponse(usuario);
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
        Grupo grupoAdministrador = grupoService.criarGruposPadrao(restaurante.getId());
        Usuario admin = usuarioRepository.save(
            Usuario.builder()
                .restaurante(restaurante)
                .grupo(grupoAdministrador)
                .donoConta(true)
                .nome(pendente.getNomeAdmin())
                .email(pendente.getEmail())
                .senha(pendente.getSenhaHash())
                .role(Role.ADMIN)
                .ativo(true)
                .emailVerificado(true)
                .build()
        );
        criarInstanciaWhatsapp(restaurante);
        Map<String, Object> contratoCriado = criarContratoBilling(
            restaurante.getId(), pendente.getPlanoId(), pendente.getModalidadeOperacao());
        criarCategoriasPadraoCatalogo(restaurante.getId());

        // Registro permanente do aceite — o registro_pendente (de onde vêm
        // esses dados) é apagado logo depois que este método retorna.
        if (pendente.getVersaoContratoAceito() != null) {
            aceiteContratoRepository.save(AceiteContrato.builder()
                .usuarioId(admin.getId())
                .restauranteId(restaurante.getId())
                .versaoContrato(pendente.getVersaoContratoAceito())
                .aceitoEm(pendente.getAceitoEm())
                .ipAceite(pendente.getIpAceite())
                .build());
        }

        // Dados reais do plano/preço/id vêm do Contrato criado acima no
        // billing-service (não de pendente, que só tem o planoId) — é o que
        // vai impresso no e-mail e no PDF anexado.
        String planoNome = null;
        java.math.BigDecimal planoPreco = null;
        Long contratoId = null;
        if (contratoCriado != null) {
            Object idObj = contratoCriado.get("id");
            contratoId = idObj != null ? ((Number) idObj).longValue() : null;
            Object planoObj = contratoCriado.get("plano");
            if (planoObj instanceof Map<?, ?> planoMap) {
                Object nomeObj = planoMap.get("nome");
                planoNome = nomeObj != null ? nomeObj.toString() : null;
                Object precoObj = planoMap.get("precoMensal");
                if (precoObj != null) planoPreco = new java.math.BigDecimal(precoObj.toString());
            }
        }
        emailService.enviarBoasVindas(pendente.getEmail(), pendente.getNomeAdmin(), restaurante.getNome(),
            planoNome, planoPreco, pendente.getVersaoContratoAceito(), pendente.getAceitoEm(), contratoId);

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

    // Retorna o Contrato criado (com o Plano aninhado) pra quem precisar dos
    // dados reais de plano/preço — ex: e-mail de boas-vindas com o PDF do
    // contrato (ver criarContaFromPendente). null se o billing-service não
    // respondeu, ou se planoId é nulo (contrato sem plano associado).
    @SuppressWarnings("unchecked")
    private Map<String, Object> criarContratoBilling(Long restauranteId, Long planoId, String modalidadeOperacao) {
        if (planoId == null) return null;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("restauranteId", restauranteId);
            body.put("planoId", planoId);
            body.put("modalidadeOperacao", modalidadeOperacao);
            return restTemplate.postForObject(
                "http://billing-service/internal/contratos",
                body,
                Map.class
            );
        } catch (Exception e) {
            log.warn("Não foi possível criar contrato no billing-service: {}", e.getMessage());
            return null;
        }
    }

    private void criarCategoriasPadraoCatalogo(Long restauranteId) {
        try {
            restTemplate.postForObject(
                "http://catalog-service/internal/categorias/padrao",
                Map.of("restauranteId", restauranteId),
                Object.class
            );
        } catch (Exception e) {
            log.warn("Não foi possível criar categorias padrão no catalog-service: {}", e.getMessage());
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
        java.util.Set<String> permissoes = permissoesEfetivas(usuario);
        String token = jwtUtil.generateToken(usuario, permissoes);
        return AuthResponse.builder()
            .token(token)
            .usuario(AuthResponse.UsuarioDto.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole().name())
                .restauranteId(usuario.getRestaurante() != null ? usuario.getRestaurante().getId() : null)
                .ativo(usuario.isAtivo())
                .grupoId(usuario.getGrupo() != null ? usuario.getGrupo().getId() : null)
                .permissoes(permissoes)
                .build())
            .build();
    }

    // Permissões do grupo, restritas ao que o plano contratado do
    // restaurante realmente inclui (ver FuncionalidadePermissoes) — usado
    // tanto pro token quanto pelo GET /me, pra manter os dois em sincronia.
    // Sem contrato/plano ou com o billing-service fora do ar, não bloqueia:
    // devolve as permissões do grupo sem filtrar.
    public java.util.Set<String> permissoesEfetivas(Usuario usuario) {
        java.util.Set<String> permissoes = usuario.getGrupo() != null ? usuario.getGrupo().getPermissoes() : null;
        if (permissoes == null || usuario.getRestaurante() == null) {
            return permissoes;
        }
        try {
            AuditoriaClient.PlanoLimitesResponse limites = billingClient.buscarLimitesPlano(usuario.getRestaurante().getId());
            return FuncionalidadePermissoes.aplicar(permissoes, limites.getFuncionalidades());
        } catch (Exception e) {
            log.warn("Não foi possível verificar recursos do plano do restaurante {}: {}",
                usuario.getRestaurante().getId(), e.getMessage());
            return permissoes;
        }
    }

    private String generateSlug(String nome) {
        return SlugUtil.normalize(nome);
    }
}
