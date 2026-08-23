package com.oiaaconta.ifood.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(errorBody(400, ex.getMessage()));
    }

    // Sem isso, uma falha de rede/API do iFood (Feign) fica sem handler e cai
    // sem controle no filtro de segurança do Spring, que devolve 401 — o
    // frontend trata QUALQUER 401 como sessão expirada e desloga o usuário
    // do próprio painel por causa de um erro que não tem nada a ver com o
    // login dele. 502 (Bad Gateway) é o status correto pra "a API de
    // terceiro que dependemos falhou" — nunca deve derrubar a sessão local.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(FeignException ex) {
        log.warn("Falha ao chamar API do iFood: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(errorBody(502, "Falha na comunicação com o iFood. Tente novamente em instantes."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field = ((FieldError) e).getField();
            errors.put(field, e.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(Map.of("status", 400, "errors", errors,
            "timestamp", LocalDateTime.now().toString()));
    }

    private Map<String, Object> errorBody(int status, String message) {
        return Map.of("status", status, "message", message, "timestamp", LocalDateTime.now().toString());
    }
}
