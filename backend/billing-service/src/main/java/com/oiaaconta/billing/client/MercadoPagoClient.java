package com.oiaaconta.billing.client;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// Encapsula o SDK do Mercado Pago (com.mercadopago:sdk-java) — cobrança de
// assinatura via Checkout Pro (Preference): o admin é redirecionado pro
// checkout hospedado do MP, paga, e o webhook confirma (ver
// PagamentoWebhookController/BillingService.confirmarPagamentoMercadoPago).
// Contrato.mpPreapprovalId fica reservado pra uma futura cobrança recorrente
// automática (API de Assinaturas do MP) — não usado aqui ainda.
@Component
@Slf4j
public class MercadoPagoClient {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${app.api-base-url:http://localhost}")
    private String apiBaseUrl;

    @Value("${app.frontend-base-url:http://localhost}")
    private String frontendBaseUrl;

    public String criarPreferenceAssinatura(String tituloPlano, BigDecimal valor, Long contratoId) {
        MercadoPagoConfig.setAccessToken(accessToken);
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Assinatura Oia a Conta — " + tituloPlano)
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(valor)
                .build();

            PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference(String.valueOf(contratoId))
                .notificationUrl(apiBaseUrl + "/api/pagamentos/webhook")
                .backUrls(PreferenceBackUrlsRequest.builder()
                    .success(frontendBaseUrl + "/admin/assinatura")
                    .pending(frontendBaseUrl + "/admin/assinatura")
                    .failure(frontendBaseUrl + "/admin/assinatura")
                    .build())
                .build();

            Preference preference = new PreferenceClient().create(request);
            return preference.getInitPoint();
        } catch (Exception e) {
            log.error("Erro ao criar cobrança Mercado Pago pro contrato {}: {}", contratoId, e.getMessage(), e);
            throw new IllegalStateException("Não foi possível gerar o link de pagamento", e);
        }
    }

    public Payment buscarPagamento(Long paymentId) {
        MercadoPagoConfig.setAccessToken(accessToken);
        try {
            return new PaymentClient().get(paymentId);
        } catch (Exception e) {
            log.error("Erro ao consultar pagamento {} no Mercado Pago: {}", paymentId, e.getMessage(), e);
            throw new IllegalStateException("Não foi possível consultar o pagamento", e);
        }
    }
}
