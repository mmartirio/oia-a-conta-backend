package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.client.NotificationClient;
import com.oiaaconta.whatsapp.dto.response.ConversaResumoResponse;
import com.oiaaconta.whatsapp.dto.response.MensagemResponse;
import com.oiaaconta.whatsapp.entity.MensagemWhatsapp;
import com.oiaaconta.whatsapp.entity.SessaoWhatsapp;
import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import com.oiaaconta.whatsapp.repository.MensagemWhatsappRepository;
import com.oiaaconta.whatsapp.repository.SessaoWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MensagemWhatsappService {

    private static final int TEXTO_MAX_LENGTH = 2000;
    private static final int JANELA_DEDUP_SEGUNDOS = 20;

    private final MensagemWhatsappRepository mensagemRepo;
    private final SessaoWhatsappRepository sessaoRepo;
    private final NotificationClient notificationClient;

    @Transactional
    public void registrar(Long restauranteId, String telefone, DirecaoMensagem direcao, String texto) {
        if (texto == null || texto.isBlank()) return;

        String textoTruncado = texto.length() > TEXTO_MAX_LENGTH
            ? texto.substring(0, TEXTO_MAX_LENGTH)
            : texto;

        MensagemWhatsapp mensagem = mensagemRepo.save(MensagemWhatsapp.builder()
            .restauranteId(restauranteId)
            .telefone(telefone)
            .direcao(direcao)
            .texto(textoTruncado)
            .lida(direcao == DirecaoMensagem.ENVIADA)
            .build());

        try {
            notificationClient.mensagemWhatsapp(NotificationClient.NotificacaoMensagemWhatsapp.builder()
                .restauranteId(restauranteId)
                .telefone(telefone)
                .direcao(direcao.name())
                .texto(textoTruncado)
                .criadoEm(String.valueOf(mensagem.getCriadoEm() != null ? mensagem.getCriadoEm() : LocalDateTime.now()))
                .build());
        } catch (Exception e) {
            log.warn("Falha ao notificar mensagem WhatsApp em tempo real: {}", e.getMessage());
        }
    }

    // Chamado pelo webhook quando o Evolution manda o espelho "fromMe" de uma
    // mensagem enviada pelo restaurante. Pode ser (a) a mesma mensagem que o
    // painel/bot já mandou e registrou na hora via registrar() acima, ou (b)
    // uma mensagem mandada direto do celular, fora do painel — que sem isso
    // nunca aparecia no histórico de Conversas. A checagem por texto+telefone
    // numa janela curta evita duplicar quando é o caso (a).
    @Transactional
    public void registrarEnviadaSeNova(Long restauranteId, String telefone, String texto) {
        if (texto == null || texto.isBlank()) return;
        String textoTruncado = texto.length() > TEXTO_MAX_LENGTH ? texto.substring(0, TEXTO_MAX_LENGTH) : texto;
        LocalDateTime desde = LocalDateTime.now().minusSeconds(JANELA_DEDUP_SEGUNDOS);
        boolean jaRegistrada = mensagemRepo.existsByRestauranteIdAndTelefoneAndDirecaoAndTextoAndCriadoEmAfter(
            restauranteId, telefone, DirecaoMensagem.ENVIADA, textoTruncado, desde);
        if (jaRegistrada) return;
        registrar(restauranteId, telefone, DirecaoMensagem.ENVIADA, texto);
    }

    public List<ConversaResumoResponse> listarConversas(Long restauranteId) {
        List<String> telefonesNaoLidos = mensagemRepo.listarTelefonesComNaoLidas(restauranteId, DirecaoMensagem.RECEBIDA);
        return mensagemRepo.findUltimaMensagemPorTelefone(restauranteId).stream()
            .map(m -> {
                SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(m.getTelefone(), restauranteId)
                    .orElse(null);
                return ConversaResumoResponse.builder()
                    .telefone(m.getTelefone())
                    .clienteNome(sessao != null ? sessao.getClienteNome() : null)
                    .numeroReal(sessao != null ? sessao.getNumeroReal() : null)
                    .ultimaMensagem(m.getTexto())
                    .direcao(m.getDirecao())
                    .criadoEm(m.getCriadoEm())
                    .naoLida(telefonesNaoLidos.contains(m.getTelefone()))
                    .build();
            })
            .toList();
    }

    public long contarConversasNaoLidas(Long restauranteId) {
        return mensagemRepo.contarConversasNaoLidas(restauranteId, DirecaoMensagem.RECEBIDA);
    }

    @Transactional
    public void marcarConversaComoLida(Long restauranteId, String telefone) {
        mensagemRepo.marcarComoLidas(restauranteId, telefone, DirecaoMensagem.RECEBIDA);
    }

    // Edição manual do nome do cliente pelo admin — necessária para conversas
    // antigas cujo nome de perfil do WhatsApp nunca foi capturado (o pushName
    // só passou a ser salvo a partir da mensagem em que essa captura foi
    // implementada; conversas anteriores a isso não têm como ser recuperadas
    // automaticamente, já que o pushName não era persistido antes).
    @Transactional
    public void renomearCliente(Long restauranteId, String telefone, String nome) {
        SessaoWhatsapp sessao = sessaoRepo.findByTelefoneAndRestauranteId(telefone, restauranteId)
            .orElseGet(() -> SessaoWhatsapp.builder()
                .telefone(telefone)
                .restauranteId(restauranteId)
                .build());
        sessao.setClienteNome(nome != null && !nome.isBlank() ? nome.trim() : null);
        sessaoRepo.save(sessao);
    }

    @Transactional
    public Page<MensagemResponse> listarMensagens(Long restauranteId, String telefone, Pageable pageable) {
        marcarConversaComoLida(restauranteId, telefone);
        return mensagemRepo.findByRestauranteIdAndTelefoneOrderByCriadoEmAsc(restauranteId, telefone, pageable)
            .map(m -> MensagemResponse.builder()
                .id(m.getId())
                .direcao(m.getDirecao())
                .texto(m.getTexto())
                .criadoEm(m.getCriadoEm())
                .build());
    }
}
