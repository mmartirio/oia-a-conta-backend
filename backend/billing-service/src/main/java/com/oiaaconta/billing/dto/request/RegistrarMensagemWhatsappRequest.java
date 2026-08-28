package com.oiaaconta.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrarMensagemWhatsappRequest {
    @NotBlank
    private String telefone;

    private String nomeContato;

    @NotBlank
    private String mensagem;
}
