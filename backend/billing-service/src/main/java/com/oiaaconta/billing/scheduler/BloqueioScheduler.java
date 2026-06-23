package com.oiaaconta.billing.scheduler;

import com.oiaaconta.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BloqueioScheduler {

    private final BillingService billingService;

    // Executa diariamente às 09:00
    @Scheduled(cron = "0 0 9 * * *")
    public void verificarInadimplentes() {
        log.info("Scheduler: verificando inadimplentes...");
        try {
            billingService.bloquearContratosPorInadimplencia();
        } catch (Exception e) {
            log.error("Erro no scheduler de bloqueio: {}", e.getMessage(), e);
        }
    }
}
