package com.oiaaconta.ifood.service;

import com.oiaaconta.ifood.client.IfoodMerchantClient;
import com.oiaaconta.ifood.client.OrderClient;
import com.oiaaconta.ifood.dto.ifood.IfoodInterrupcaoRequest;
import com.oiaaconta.ifood.dto.ifood.IfoodInterrupcaoResponse;
import com.oiaaconta.ifood.dto.order.StatusFuncionamentoDto;
import com.oiaaconta.ifood.entity.IfoodMerchant;
import com.oiaaconta.ifood.repository.IfoodMerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

// Mantém o status aberto/fechado da loja no iFood em linha com o status
// aqui (manual + pausa programada + horário semanal — GET
// /api/configuracoes/pausas/status no order-service, já público e feito
// exatamente pra essa pergunta). "Fechar agora" no iFood é criar uma
// interrupção temporária; reabrir é remover essa interrupção antes do fim
// programado — não existe um "toggle" direto de disponibilidade na API deles.
@Service
@RequiredArgsConstructor
@Slf4j
public class IfoodStatusSyncService {

    private final OrderClient orderClient;
    private final IfoodMerchantClient ifoodMerchantClient;
    private final IfoodMerchantRepository merchantRepository;
    private final IfoodVinculoService vinculoService;

    public void sincronizar(IfoodMerchant merchant) {
        StatusFuncionamentoDto statusLocal = orderClient.statusPausa(merchant.getRestauranteId());
        boolean abertoLocal = statusLocal.isAberto();
        boolean abertoNoIfoodAtualmente = merchant.getInterrupcaoAtivaId() == null;

        if (merchant.getUltimoStatusEnviado() != null
            && merchant.getUltimoStatusEnviado() == abertoLocal
            && abertoNoIfoodAtualmente == abertoLocal) {
            return;
        }

        String token = "Bearer " + vinculoService.garantirTokenValido(merchant);

        if (abertoLocal && merchant.getInterrupcaoAtivaId() != null) {
            ifoodMerchantClient.removerInterrupcao(token, merchant.getMerchantId(), merchant.getInterrupcaoAtivaId());
            merchant.setInterrupcaoAtivaId(null);
        } else if (!abertoLocal && merchant.getInterrupcaoAtivaId() == null) {
            OffsetDateTime agora = OffsetDateTime.now();
            IfoodInterrupcaoRequest body = new IfoodInterrupcaoRequest(
                agora.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                agora.plus(Duration.ofHours(24)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                statusLocal.getMotivo() != null ? statusLocal.getMotivo() : "Loja fechada");
            IfoodInterrupcaoResponse resp = ifoodMerchantClient.criarInterrupcao(token, merchant.getMerchantId(), body);
            merchant.setInterrupcaoAtivaId(resp.getId());
        }

        merchant.setUltimoStatusEnviado(abertoLocal);
        merchantRepository.save(merchant);
    }
}
