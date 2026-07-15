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
public class MensagemResponse {
    private Long id;
    private DirecaoMensagem direcao;
    private String texto;
    private LocalDateTime criadoEm;
}
