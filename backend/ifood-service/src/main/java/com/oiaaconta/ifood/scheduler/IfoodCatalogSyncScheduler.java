package com.oiaaconta.ifood.scheduler;

import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodCatalogSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Sincroniza o cardápio de todo restaurante vinculado periodicamente, além
// do botão "sincronizar agora" no admin — não existe hoje nenhum
// evento/webhook no catalog-service quando um produto muda, então o
// caminho mais simples é essa varredura.
@Component
@RequiredArgsConstructor
@Slf4j
public class IfoodCatalogSyncScheduler {

    private final IfoodMerchantRepository merchantRepository;
    private final IfoodCatalogSyncService catalogSyncService;

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT1M")
    public void sincronizarTodos() {
        List<IfoodMerchant> merchants = merchantRepository.findByAtivoTrue();
        for (IfoodMerchant merchant : merchants) {
            try {
                catalogSyncService.sincronizar(merchant.getRestauranteId());
            } catch (Exception e) {
                log.warn("Falha ao sincronizar catálogo iFood do restaurante {}: {}", merchant.getRestauranteId(), e.getMessage());
            }
        }
    }
}
