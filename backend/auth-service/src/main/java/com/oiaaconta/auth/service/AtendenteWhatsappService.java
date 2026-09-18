package com.oiaaconta.auth.service;

import com.oiaaconta.auth.client.AuditoriaClient;
import com.oiaaconta.auth.exception.BusinessException;
import com.oiaaconta.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

// "Atendente de WhatsApp" não é uma entidade própria — é qualquer Usuario
// ativo cujo Grupo tem permissão WHATSAPP_CONVERSAS ou WHATSAPP_MENSAGENS.
// O limite (por plano/modalidade) vive no billing-service; aqui só conta
// e valida antes de conceder a permissão via Grupo ou atribuição de Usuario.
@Service
@RequiredArgsConstructor
@Slf4j
public class AtendenteWhatsappService {

    private static final Set<String> PERMISSOES_ATENDIMENTO = Set.of("WHATSAPP_CONVERSAS", "WHATSAPP_MENSAGENS");

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaClient billingClient;

    public boolean temPermissaoAtendimento(Set<String> permissoes) {
        return permissoes != null && permissoes.stream().anyMatch(PERMISSOES_ATENDIMENTO::contains);
    }

    public long contarAtendentesAtivos(Long restauranteId) {
        return usuarioRepository.findByRestauranteIdAndAtivoTrue(restauranteId).stream()
            .filter(u -> u.getGrupo() != null && temPermissaoAtendimento(u.getGrupo().getPermissoes()))
            .count();
    }

    // Lança BusinessException se a quantidade projetada (depois da mudança
    // que está prestes a ser salva) estourar o limite do plano.
    public void validarNovoTotal(Long restauranteId, long quantidadeProjetada) {
        Integer limite = buscarLimite(restauranteId);
        if (limite != null && quantidadeProjetada > limite) {
            throw new BusinessException(limite == 0
                ? "Seu plano não inclui atendimento por WhatsApp."
                : "Seu plano permite no máximo " + limite + " atendente(s) de WhatsApp.");
        }
    }

    // Indisponibilidade do billing-service não pode impedir a gestão normal
    // de usuários/grupos — falha aberta (sem limite).
    private Integer buscarLimite(Long restauranteId) {
        try {
            AuditoriaClient.RestricoesOperacaoResponse dto = billingClient.buscarRestricoesOperacao(restauranteId);
            return dto != null ? dto.getLimiteAtendentesWhatsapp() : null;
        } catch (Exception e) {
            log.warn("Não foi possível consultar limite de atendentes de WhatsApp do restaurante {}: {}", restauranteId, e.getMessage());
            return null;
        }
    }
}
