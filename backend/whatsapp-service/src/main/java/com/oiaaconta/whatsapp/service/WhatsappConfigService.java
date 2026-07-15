package com.oiaaconta.whatsapp.service;

import com.oiaaconta.whatsapp.entity.WhatsappConfig;
import com.oiaaconta.whatsapp.repository.WhatsappConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsappConfigService {

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
}
