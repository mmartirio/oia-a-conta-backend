package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.entity.WhatsappConfig;
import com.oiaaconta.whatsapp.repository.WhatsappConfigRepository;
import com.oiaaconta.whatsapp.util.ImagemValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsappConfigService {

    // ~1MB de imagem original vira ~1,2M de caracteres em base64.
    private static final int IMAGEM_MAX_CHARS = 1_200_000;

    private final WhatsappConfigRepository configRepository;

    @SuppressWarnings("null")
    public boolean isChatbotAtivo(Long restauranteId) {
        return configRepository.findByRestauranteId(restauranteId)
            .map(WhatsappConfig::isChatbotAtivo)
            .orElse(true);
    }

    @Transactional
    public boolean atualizarChatbotAtivo(Long restauranteId, boolean ativo) {
        WhatsappConfig config = configRepository.findByRestauranteId(restauranteId)
            .orElseGet(() -> WhatsappConfig.builder().restauranteId(restauranteId).build());
        config.setChatbotAtivo(ativo);
        return configRepository.save(config).isChatbotAtivo();
    }

    // Null = admin ainda não subiu a imagem do cardápio numerado — o lembrete
    // de 10 min manda só o texto nesse caso (ver ChatbotService).
    public String getImagemCardapio(Long restauranteId) {
        return configRepository.findByRestauranteId(restauranteId)
            .map(WhatsappConfig::getImagemCardapioBase64)
            .orElse(null);
    }

    @Transactional
    public void atualizarImagemCardapio(Long restauranteId, String imagemBase64) {
        String valor = imagemBase64 != null && imagemBase64.isBlank() ? null : imagemBase64;
        if (valor != null) {
            ImagemValidator.validar(valor);
            if (valor.length() > IMAGEM_MAX_CHARS) {
                throw new IllegalArgumentException("Imagem muito grande. Envie um arquivo menor (até ~1MB).");
            }
        }
        WhatsappConfig config = configRepository.findByRestauranteId(restauranteId)
            .orElseGet(() -> WhatsappConfig.builder().restauranteId(restauranteId).build());
        config.setImagemCardapioBase64(valor);
        configRepository.save(config);
    }
}
