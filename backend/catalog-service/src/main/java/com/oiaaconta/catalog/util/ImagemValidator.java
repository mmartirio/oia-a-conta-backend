package com.oiaaconta.catalog.util;

import com.oiaaconta.catalog.exception.BusinessException;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Validação de imagem enviada como data URI base64 — checa o tipo declarado E
// os magic bytes reais do conteúdo decodificado, não só o prefixo "data:image/"
// (um "data:image/svg+xml;base64,..." passava antes disso, por exemplo).
public final class ImagemValidator {

    private ImagemValidator() { }

    private static final Pattern DATA_URI = Pattern.compile("^data:(image/[a-zA-Z+.-]+);base64,(.+)$", Pattern.DOTALL);

    public static void validar(String dataUri) {
        Matcher m = DATA_URI.matcher(dataUri);
        if (!m.matches()) {
            throw new BusinessException("Arquivo inválido. Envie uma imagem (PNG, JPG ou WEBP).");
        }
        String tipo = m.group(1);
        if (!tipo.equals("image/jpeg") && !tipo.equals("image/png") && !tipo.equals("image/webp")) {
            throw new BusinessException("Formato de imagem não suportado. Envie PNG, JPG ou WEBP.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Arquivo inválido. Envie uma imagem (PNG, JPG ou WEBP).");
        }
        if (!assinaturaBate(tipo, bytes)) {
            throw new BusinessException("O conteúdo do arquivo não corresponde a uma imagem válida.");
        }
    }

    private static boolean assinaturaBate(String tipo, byte[] b) {
        return switch (tipo) {
            case "image/jpeg" -> b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "image/png" -> b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
            case "image/webp" -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
