package com.oiaaconta.whatsapp.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MensagemTemplateDto {
    private String chave;
    private String label;
    private String texto;
    private String textoPadrao;
    private boolean personalizado;
    private boolean sistema;
    private String grupo;
    private int ordem;
    private String variavelHint;
    private boolean ativo; // false = mensagem desativada (removida) pelo usuário
}
