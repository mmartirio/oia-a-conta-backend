package com.oiaaconta.whatsapp.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

// Sem isso, validações de negócio (ex: imagem muito grande/formato inválido em
// ImagemValidator/WhatsappConfigService) escapavam como IllegalArgumentException
// não tratada — virava 500 genérico pro frontend, escondendo a mensagem real
// que explicaria o problema pro usuário.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", 400,
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
