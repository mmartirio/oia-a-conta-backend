package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.EvolutionApiClient;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final SessaoWhatsappRepository sessaoRepo;
    private final EvolutionApiClient evolutionClient;
    private final MensagemWhatsappService mensagemWhatsappService;

    public void notificarPorEntregaId(Long entregaId, String mensagem) {
        sessaoRepo.findByEntregaId(entregaId).ifPresentOrElse(
            sessao -> {
                evolutionClient.enviarMensagem(sessao.getTelefone(), mensagem);
                mensagemWhatsappService.registrar(sessao.getRestauranteId(), sessao.getTelefone(),
                    DirecaoMensagem.ENVIADA, mensagem);
                log.info("Notificação enviada para {} (entrega #{})", sessao.getTelefone(), entregaId);
            },
            () -> log.warn("Sessão não encontrada para entrega #{}", entregaId)
        );
    }
}
