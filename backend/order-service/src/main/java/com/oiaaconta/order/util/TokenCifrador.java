package com.oiaaconta.order.util;

import com.oiaaconta.order.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Cifra os tokens OAuth do Mercado Pago dos restaurantes antes de gravar no
// banco (AES-256-GCM, IV aleatório por valor). A chave vem de MP_TOKEN_KEY
// — trocar essa variável invalida todos os tokens salvos (os restaurantes
// precisam reconectar), então não mexer nela depois de configurada.
@Component
public class TokenCifrador {

    private static final int IV_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec chave;

    public TokenCifrador(@Value("${mercadopago.token-key:}") String segredo) {
        if (segredo == null || segredo.isBlank()) {
            this.chave = null;
            return;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean configurado() {
        return chave != null;
    }

    public String cifrar(String texto) {
        if (texto == null) return null;
        exigirChave();
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(128, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar token", e);
        }
    }

    public String decifrar(String valor) {
        if (valor == null) return null;
        exigirChave();
        try {
            byte[] dados = Base64.getDecoder().decode(valor);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(128, dados, 0, IV_BYTES));
            return new String(cipher.doFinal(dados, IV_BYTES, dados.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar token (MP_TOKEN_KEY mudou?)", e);
        }
    }

    private void exigirChave() {
        if (chave == null) {
            throw new BusinessException("Pagamento online não configurado no servidor (MP_TOKEN_KEY ausente).");
        }
    }
}
