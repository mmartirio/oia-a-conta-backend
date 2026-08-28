package com.oiaaconta.billing.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Validação do webhook do Mercado Pago — formato documentado do header
// "x-signature": "ts=<timestamp>,v1=<hash>". O hash é um HMAC-SHA256 sobre o
// manifest "id:<dataId>;request-id:<xRequestId>;ts:<ts>;", com a chave sendo
// o webhook secret configurado no painel do MP. Sem isso, qualquer um
// poderia forjar uma notificação de pagamento aprovado.
public final class MercadoPagoSignature {

    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("ts=([^,]+),v1=([^,]+)");

    private MercadoPagoSignature() {}

    public static boolean valida(String xSignature, String xRequestId, String dataId, String secret) {
        if (xSignature == null || xRequestId == null || dataId == null
                || secret == null || secret.isBlank()) {
            return false;
        }
        Matcher m = SIGNATURE_PATTERN.matcher(xSignature);
        if (!m.find()) return false;
        String ts = m.group(1);
        String v1 = m.group(2);

        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperado = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
            return esperado.equalsIgnoreCase(v1);
        } catch (Exception e) {
            return false;
        }
    }
}
