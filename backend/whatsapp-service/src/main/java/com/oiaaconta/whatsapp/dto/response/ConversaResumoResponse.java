package com.oiaaconta.whatsapp.dto.response;

import com.oiaaconta.whatsapp.enums.DirecaoMensagem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversaResumoResponse {
    private String telefone;
    private String clienteNome;
    private String numeroReal;
    private String ultimaMensagem;
    private DirecaoMensagem direcao;
    private LocalDateTime criadoEm;
    private boolean naoLida;
}
