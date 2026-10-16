package com.oiaaconta.ifood.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// Cifra os tokens OAuth do iFood no banco (AES-256-GCM, chave derivada de
// TOKEN_ENCRYPTION_KEY). Valores gravados antes disso (texto puro) continuam
// legíveis — sem o prefixo "enc:" são devolvidos como estão — e passam a ser
// cifrados na próxima vez que o token for renovado/salvo.
// Sem TOKEN_ENCRYPTION_KEY, grava em texto puro (com aviso) pra não quebrar
// o ambiente local; em produção a variável é obrigatória (docker-compose).
@Converter
@Slf4j
public class TokenCriptografadoConverter implements AttributeConverter<String, String> {

    private static final String PREFIXO = "enc:";
    private static final int IV_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SecretKeySpec CHAVE = carregarChave();

    private static SecretKeySpec carregarChave() {
        String segredo = System.getenv("TOKEN_ENCRYPTION_KEY");
        if (segredo == null || segredo.isBlank()) {
            log.warn("TOKEN_ENCRYPTION_KEY ausente — tokens do iFood serão gravados SEM criptografia");
            return null;
        }
        try {
            return new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                .digest(segredo.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String valor) {
        if (valor == null || CHAVE == null) return valor;
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, CHAVE, new GCMParameterSpec(128, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            return PREFIXO + Base64.getEncoder().encodeToString(
                ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar token do iFood", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        if (valor == null || !valor.startsWith(PREFIXO)) return valor;
        if (CHAVE == null) throw new IllegalStateException("Token do iFood cifrado mas TOKEN_ENCRYPTION_KEY ausente");
        try {
            byte[] dados = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, CHAVE, new GCMParameterSpec(128, dados, 0, IV_BYTES));
            return new String(cipher.doFinal(dados, IV_BYTES, dados.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar token do iFood (TOKEN_ENCRYPTION_KEY mudou?)", e);
        }
    }
}
