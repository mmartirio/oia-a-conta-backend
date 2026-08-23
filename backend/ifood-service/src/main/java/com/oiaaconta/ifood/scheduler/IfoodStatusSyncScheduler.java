package com.oiaaconta.ifood.scheduler;

import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import com.oiaaconta.ifood.service.IfoodStatusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IfoodStatusSyncScheduler {

    private final IfoodMerchantRepository merchantRepository;
    private final IfoodStatusSyncService statusSyncService;

    @Scheduled(fixedDelayString = "${ifood.status-sync-interval-ms:120000}")
    public void executar() {
        for (IfoodMerchant merchant : merchantRepository.findByAtivoTrue()) {
            try {
                statusSyncService.sincronizar(merchant);
            } catch (Exception e) {
                log.warn("Falha ao sincronizar status da loja iFood do restaurante {}: {}", merchant.getRestauranteId(), e.getMessage());
            }
        }
    }
}
