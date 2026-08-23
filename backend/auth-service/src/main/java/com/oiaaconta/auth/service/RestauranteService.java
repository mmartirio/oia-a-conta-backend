package com.oiaaconta.auth.service;

import com.oiaaconta.auth.dto.request.BackgroundRequest;
import com.oiaaconta.auth.dto.request.LogoRequest;
import com.oiaaconta.auth.dto.request.RestauranteCoresRequest;
import com.oiaaconta.auth.dto.request.RestauranteLocalizacaoRequest;
import com.oiaaconta.auth.dto.request.RestauranteUpdateRequest;
import com.oiaaconta.auth.dto.response.RestauranteDadosResponse;
import com.oiaaconta.auth.entity.Restaurante;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.exception.ResourceNotFoundException;
import com.oiaaconta.auth.repository.RestauranteRepository;
import com.oiaaconta.auth.util.ImagemValidator;
import com.oiaaconta.auth.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RestauranteService {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    // ~1.5MB de imagem original vira ~2MB em base64 — teto generoso para uma logo.
    private static final int LOGO_MAX_CHARS = 2_100_000;

    private final RestauranteRepository restauranteRepository;

    public RestauranteDadosResponse buscarDados(@NonNull Long restauranteId) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        return toResponse(restaurante);
    }

    @Transactional
    @SuppressWarnings("null")
    public RestauranteDadosResponse atualizarDados(@NonNull Long restauranteId, RestauranteUpdateRequest request) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));

        // Dados cadastrais/endereço + slug (link público do cardápio). plano/ativo/bloqueado/
        // whatsappInstanceName permanecem exclusivos de outros fluxos (billing, bloqueio, integração WhatsApp).
        boolean nomeMudou = request.getNome() != null && !request.getNome().equals(restaurante.getNome());
        if (request.getNome() != null) restaurante.setNome(request.getNome());

        if (nomeMudou) {
            // O link segue o nome sempre que o nome muda — mesmo se um slug manual também
            // vier na mesma requisição, o nome tem prioridade (decisão do usuário).
            restaurante.setSlug(gerarSlugUnico(request.getNome(), restauranteId));
        } else if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String novoSlug = SlugUtil.normalize(request.getSlug());
            if (novoSlug.isBlank()) {
                throw new BusinessException("Link inválido. Use apenas letras, números e hífens.");
            }
            if (!novoSlug.equals(restaurante.getSlug())
                    && restauranteRepository.existsBySlugAndIdNot(novoSlug, restauranteId)) {
                throw new BusinessException("Este link já está em uso por outro restaurante. Escolha outro.");
            }
            restaurante.setSlug(novoSlug);
        }
        if (request.getCnpj() != null) restaurante.setCnpj(request.getCnpj());
        if (request.getTelefone() != null) restaurante.setTelefone(request.getTelefone());
        if (request.getEnderecoRua() != null) restaurante.setEnderecoRua(request.getEnderecoRua());
        if (request.getEnderecoNumero() != null) restaurante.setEnderecoNumero(request.getEnderecoNumero());
        if (request.getEnderecoBairro() != null) restaurante.setEnderecoBairro(request.getEnderecoBairro());
        if (request.getEnderecoCidade() != null) restaurante.setEnderecoCidade(request.getEnderecoCidade());
        if (request.getEnderecoComplemento() != null) restaurante.setEnderecoComplemento(request.getEnderecoComplemento());

        return toResponse(restauranteRepository.save(restaurante));
    }

    @Transactional
    @SuppressWarnings("null")
    public RestauranteDadosResponse atualizarCores(@NonNull Long restauranteId, RestauranteCoresRequest request) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));

        // Cores: string vazia limpa (volta ao padrão da marca), string não-vazia precisa ser hex válido.
        restaurante.setCorPrimaria(validarCorOuLimpar(request.getCorPrimaria(), restaurante.getCorPrimaria()));
        restaurante.setCorSecundaria(validarCorOuLimpar(request.getCorSecundaria(), restaurante.getCorSecundaria()));
        restaurante.setCorAccent(validarCorOuLimpar(request.getCorAccent(), restaurante.getCorAccent()));
        restaurante.setCorTexto(validarCorOuLimpar(request.getCorTexto(), restaurante.getCorTexto()));

        return toResponse(restauranteRepository.save(restaurante));
    }

    private String gerarSlugUnico(String nome, Long restauranteId) {
        String base = SlugUtil.normalize(nome);
        if (base.isBlank()) {
            throw new BusinessException("Não foi possível gerar o link a partir desse nome. Tente um nome com letras ou números.");
        }
        String slug = base;
        if (restauranteRepository.existsBySlugAndIdNot(slug, restauranteId)) {
            slug = base + "-" + System.currentTimeMillis();
        }
        return slug;
    }

    @Transactional
    @SuppressWarnings("null")
    public RestauranteDadosResponse atualizarLocalizacao(@NonNull Long restauranteId, RestauranteLocalizacaoRequest request) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        restaurante.setLatitude(request.getLatitude());
        restaurante.setLongitude(request.getLongitude());
        return toResponse(restauranteRepository.save(restaurante));
    }

    private String validarCorOuLimpar(String novaCor, String corAtual) {
        if (novaCor == null) return corAtual;
        if (novaCor.isBlank()) return null;
        if (!HEX_COLOR.matcher(novaCor.trim()).matches()) {
            throw new BusinessException("Cor inválida: \"" + novaCor + "\". Use o formato hexadecimal, ex: #CF4622.");
        }
        return novaCor.trim();
    }

    @Transactional
    public RestauranteDadosResponse atualizarLogo(@NonNull Long restauranteId, LogoRequest request) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        restaurante.setLogoBase64(validarImagemOuLimpar(request.getLogoBase64()));
        return toResponse(restauranteRepository.save(restaurante));
    }

    @Transactional
    public RestauranteDadosResponse atualizarBackground(@NonNull Long restauranteId, BackgroundRequest request) {
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        restaurante.setBackgroundBase64(validarImagemOuLimpar(request.getBackgroundBase64()));

        if (request.getBackgroundOpacidade() != null) {
            int op = request.getBackgroundOpacidade();
            if (op < 0 || op > 100) {
                throw new BusinessException("Opacidade inválida. Use um valor entre 0 e 100.");
            }
            restaurante.setBackgroundOpacidade(op);
        } else if (restaurante.getBackgroundBase64() != null && restaurante.getBackgroundOpacidade() == null) {
            // Primeira imagem de fundo enviada sem opacidade explícita: sugere um valor discreto por padrão.
            restaurante.setBackgroundOpacidade(15);
        }

        return toResponse(restauranteRepository.save(restaurante));
    }

    private String validarImagemOuLimpar(String imagem) {
        if (imagem == null || imagem.isBlank()) return null;
        ImagemValidator.validar(imagem);
        if (imagem.length() > LOGO_MAX_CHARS) {
            throw new BusinessException("Imagem muito grande. Envie um arquivo menor (até ~1,5MB).");
        }
        return imagem;
    }

    private RestauranteDadosResponse toResponse(Restaurante r) {
        return RestauranteDadosResponse.builder()
            .nome(r.getNome())
            .slug(r.getSlug())
            .plano(r.getPlano())
            .ativo(r.isAtivo())
            .cnpj(r.getCnpj())
            .telefone(r.getTelefone())
            .enderecoRua(r.getEnderecoRua())
            .enderecoNumero(r.getEnderecoNumero())
            .enderecoBairro(r.getEnderecoBairro())
            .enderecoCidade(r.getEnderecoCidade())
            .enderecoComplemento(r.getEnderecoComplemento())
            .logoBase64(r.getLogoBase64())
            .corPrimaria(r.getCorPrimaria())
            .corSecundaria(r.getCorSecundaria())
            .corAccent(r.getCorAccent())
            .corTexto(r.getCorTexto())
            .backgroundBase64(r.getBackgroundBase64())
            .backgroundOpacidade(r.getBackgroundOpacidade())
            .latitude(r.getLatitude())
            .longitude(r.getLongitude())
            .build();
    }
}
