package com.oiaaconta.whatsapp.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WhatsappStatusDto {
    private String estado;        // CONECTADO, DESCONECTADO, AGUARDANDO_SCAN, ERRO
    private String qrCodeBase64;  // data:image/png;base64,...
    private String qrCodeRaw;     // raw QR string para fallback
    private String mensagem;      // mensagem de erro, se houver
}
